#!/usr/bin/env python3
"""Validate and apply a pinned, controlled source export. Never performs a push."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import stat
import subprocess
import sys
import tempfile

from public_guard import audit_files, load_restricted_terms

MANIFEST = ".youriptv-sync.json"
SOURCE = "youriptvus/YourIPTV-GooglePlay"
DESTINATION = "youriptvus/YourIPTV"
POLICY_PATH = ".sync/export-policy.json"


def git(root: Path, *args: str, input_bytes=None) -> bytes:
    result = subprocess.run(
        ["git", "--no-optional-locks", "-c", f"safe.directory={root.resolve().as_posix()}",
         "-c", "core.hooksPath=", "-C", str(root), *args],
        input=input_bytes, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False,
    )
    if result.returncode:
        raise ValueError("Git validation failed; inspect the repository locally.")
    return result.stdout


def digest(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def checkout_matches(value: bytes, expected: str) -> bool:
    # Git may materialize committed LF text as CRLF on Windows. Preserve the
    # committed digest without treating a clean checkout's EOL conversion as drift.
    return digest(value) == expected or (b"\0" not in value and digest(value.replace(b"\r\n", b"\n")) == expected)


def path_ok(name: str) -> None:
    if not isinstance(name, str) or "\\" in name or any(ord(c) < 32 or c in ':<>"|?*' for c in name):
        raise ValueError("Unsafe export path.")
    parts = PurePosixPath(name).parts
    if not parts or name.startswith("/") or any(p.casefold() in (".", "..", ".git") for p in parts):
        raise ValueError("Unsafe export path.")
    if PurePosixPath(name).as_posix() != name:
        raise ValueError("Non-canonical export path.")
    for part in parts:
        if part.rstrip(". ") != part or re.fullmatch(r"(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\..*)?", part):
            raise ValueError("Unsafe platform path.")


def contained(root: Path, name: str) -> Path:
    path_ok(name)
    root = root.resolve(strict=True)
    target = root.joinpath(*PurePosixPath(name).parts)
    for path in [target, *target.parents]:
        if path == root:
            break
        if path.is_symlink() or (hasattr(path, "is_junction") and path.is_junction()):
            raise ValueError("Symlinks and junctions are not export destinations.")
    try:
        target.resolve().relative_to(root)
    except ValueError:
        raise ValueError("Export path escapes the destination.") from None
    return target


def read_tree(root: Path, revision: str) -> tuple[str, dict[str, tuple[bytes, int]]]:
    commit = git(root, "rev-parse", "--verify", f"{revision}^{{commit}}").decode().strip()
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("A pinned commit is required.")
    entries = []
    for record in git(root, "ls-tree", "-rz", "--full-tree", commit).split(b"\0"):
        if not record:
            continue
        metadata, name_bytes = record.split(b"\t", 1)
        mode, kind, oid = metadata.decode("ascii").split()
        name = name_bytes.decode("utf-8", errors="strict")
        path_ok(name)
        if kind != "blob" or mode not in ("100644", "100755"):
            raise ValueError("Git submodules and symbolic links require explicit review.")
        entries.append((name, oid, int(mode, 8)))
    request = "".join(oid + "\n" for _, oid, _ in entries).encode("ascii")
    response = git(root, "cat-file", "--batch", input_bytes=request)
    offset = 0
    files = {}
    for name, oid, mode in entries:
        end = response.index(b"\n", offset)
        header = response[offset:end].split()
        if len(header) != 3 or header[0].decode() != oid or header[1] != b"blob":
            raise ValueError("Git object validation failed.")
        size = int(header[2])
        start = end + 1
        files[name] = (response[start:start + size], mode)
        offset = start + size + 1
    return commit, files


def load_policy(tree):
    policy = json.loads(tree[POLICY_PATH][0])
    if (policy.get("schema") != 1 or policy.get("sourceRepository") != SOURCE
            or policy.get("destinationRepository") != DESTINATION
            or policy.get("sourceBranch") != "main" or policy.get("destinationBranch") != "main"):
        raise ValueError("Unexpected synchronization identity.")
    for name in policy["files"]:
        path_ok(name)
        if name.startswith(".github/") or name == MANIFEST:
            raise ValueError("Workflows and synchronization ownership cannot be mirrored.")
    if policy.get("trees") != ["app/src/"]:
        raise ValueError("Application source export roots require explicit code review.")
    return policy


def managed(name, policy):
    return name in policy["files"] or any(name.startswith(prefix) for prefix in policy["trees"])


def public_snapshot(tree):
    """Audit all current tracked public content, including source excluded from export."""
    return {name: value for name, (value, _) in tree.items()}


def export_tree(tree, policy, terms):
    payload = {name: item for name, item in tree.items() if managed(name, policy)}
    # Transforms are deliberately limited to the customer distribution identity and README.
    if policy.get("transforms") != {"README.md": "distribution/customer/README.md",
                                    "distribution.properties": "distribution=customer\n"}:
        raise ValueError("Unexpected distribution transformation.")
    payload["README.md"] = tree["distribution/customer/README.md"]
    payload["distribution.properties"] = (b"distribution=customer\n", 0o100644)
    for required in ("app/build.gradle.kts", "version.properties", "distribution.properties", "gradlew"):
        if required not in payload:
            raise ValueError("Required build source is missing.")
    validate_public({name: data for name, (data, _) in payload.items()}, terms)
    return payload


def validate_public(files, terms):
    if not terms:
        raise ValueError("Private restricted-term configuration is required.")
    report = audit_files(files, terms)
    if not report["passed"]:
        # Guard findings contain only sanitized paths and categories, never content.
        print(json.dumps(report, sort_keys=True), file=sys.stderr)
        raise ValueError("Public-content validation failed.")
    return report


def clean_destination(root):
    if git(root, "status", "--porcelain=v1", "--untracked-files=all"):
        raise ValueError("Destination has local changes; preserve them before synchronization.")


def plan_export(destination: Path, source_commit: str, payload, policy):
    clean_destination(destination)
    head = git(destination, "rev-parse", "HEAD").decode().strip()
    manifest_path = contained(destination, MANIFEST)
    bootstrap = not manifest_path.exists()
    if bootstrap:
        if head != policy["bootstrapBaseline"]:
            raise ValueError("Initial export requires the reviewed destination baseline.")
        _, old_tree = read_tree(destination, head)
        old = {name: {"sha256": digest(data), "mode": mode}
               for name, (data, mode) in old_tree.items() if managed(name, policy)}
    else:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        if manifest.get("schema") != 1 or manifest.get("sourceRepository") != SOURCE:
            raise ValueError("Unexpected synchronization ownership manifest.")
        old = manifest["files"]
        if not isinstance(old, dict):
            raise ValueError("Invalid ownership manifest.")
        _, committed_tree = read_tree(destination, head)
        if any(name not in committed_tree or committed_tree[name][1] != record.get("mode")
               for name, record in old.items()):
            raise ValueError("Destination executable mode drift detected.")
    names = set(old) | set(payload)
    if len({name.casefold() for name in names}) != len(names):
        raise ValueError("Export contains platform filename collisions.")
    updates, deletions = [], []
    for name in sorted(names):
        path_ok(name)
        if not managed(name, policy):
            raise ValueError("Ownership manifest claims an unmanaged path.")
        target = contained(destination, name)
        if name in old:
            if not target.is_file() or not checkout_matches(target.read_bytes(), old[name]["sha256"]):
                raise ValueError("Destination drift detected; preserve and review the change.")
        elif target.exists():
            raise ValueError("Export conflicts with an unmanaged destination file.")
        if name not in payload:
            deletions.append(name)
        elif name not in old or digest(payload[name][0]) != old[name]["sha256"] or payload[name][1] != old[name].get("mode"):
            updates.append(name)
    changed = bootstrap or bool(updates or deletions)
    manifest_bytes = (json.dumps({
        "schema": 1, "sourceRepository": SOURCE, "sourceCommit": source_commit,
        "files": {name: {"sha256": digest(data), "mode": mode} for name, (data, mode) in sorted(payload.items())},
    }, indent=2, sort_keys=True) + "\n").encode()
    return {"sourceCommit": source_commit, "destinationCommit": head, "bootstrap": bootstrap,
            "changed": changed, "updates": updates, "deletions": deletions,
            "managedPaths": sorted(payload), "manifestBytes": manifest_bytes,
            "preconditions": {name: digest(contained(destination, name).read_bytes())
                              if contained(destination, name).is_file() else None for name in names | {MANIFEST}}}


def apply_export(destination, payload, plan):
    # Preflight the complete write/delete set before performing any mutation.
    clean_destination(destination)
    if git(destination, "rev-parse", "HEAD").decode().strip() != plan["destinationCommit"]:
        raise ValueError("Destination changed after validation.")
    if not plan["changed"]:
        return
    for name, expected in plan["preconditions"].items():
        target = contained(destination, name)
        actual = digest(target.read_bytes()) if target.is_file() else None
        if actual != expected or (expected is None and target.exists()):
            raise ValueError("Destination files changed after validation.")
    names = plan["updates"] + plan["deletions"] + [MANIFEST]
    targets = {name: contained(destination, name) for name in names}
    before = {name: (p.read_bytes(), p.stat().st_mode) if p.exists() else None for name, p in targets.items()}
    try:
        for name in plan["updates"] + [MANIFEST]:
            target = targets[name]
            target.parent.mkdir(parents=True, exist_ok=True)
            data, mode = (plan["manifestBytes"], 0o100644) if name == MANIFEST else payload[name]
            with tempfile.NamedTemporaryFile(dir=target.parent, prefix=".sync-", delete=False) as stream:
                temporary = Path(stream.name)
                stream.write(data)
            try:
                temporary.chmod(stat.S_IMODE(mode))
                temporary.replace(target)
            finally:
                temporary.unlink(missing_ok=True)
        for name in plan["deletions"]:
            # contained() verifies the resolved absolute file remains inside the destination.
            contained(destination, name).unlink()
    except BaseException:
        for name, snapshot in before.items():
            target = contained(destination, name)
            if snapshot is None:
                target.unlink(missing_ok=True)
            else:
                target.write_bytes(snapshot[0])
                target.chmod(stat.S_IMODE(snapshot[1]))
        raise


def reportable(plan):
    return {key: value for key, value in plan.items() if key not in ("manifestBytes", "preconditions")}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("audit", "plan", "apply"))
    parser.add_argument("--source", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--revision", default="HEAD")
    parser.add_argument("--destination", type=Path)
    parser.add_argument("--terms-file", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    try:
        terms = load_restricted_terms(args.terms_file)
        commit, tree = read_tree(args.source, args.revision)
        validate_public(public_snapshot(tree), terms)
        policy = load_policy(tree)
        payload = export_tree(tree, policy, terms)
        if args.command == "audit":
            result = {"passed": True, "sourceCommit": commit, "exportedFiles": len(payload)}
        else:
            if not args.destination:
                raise ValueError("A destination checkout is required.")
            plan = plan_export(args.destination, commit, payload, policy)
            # Audit the resulting tracked public tree, including preserved destination workflows.
            _, destination_tree = read_tree(args.destination, plan["destinationCommit"])
            candidate = {name: value for name, value in destination_tree.items() if name not in plan["deletions"]}
            candidate.update(payload)
            validate_public(public_snapshot(candidate), terms)
            if args.command == "apply":
                apply_export(args.destination, payload, plan)
            result = reportable(plan)
        if args.report:
            args.report.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(result, indent=2))
        return 0
    except (ValueError, KeyError, OSError, UnicodeError) as error:
        print("Synchronization stopped: " + (str(error) if isinstance(error, ValueError) else "Invalid source/configuration or filesystem state."), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
