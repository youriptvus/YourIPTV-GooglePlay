#!/usr/bin/env python3
"""Destination writer for the single authorized hosted Actions workflow only."""
from __future__ import annotations

import base64
import json
import os
from pathlib import Path
import re
import subprocess
import sys

from public_guard import load_restricted_terms
from sync_repository import (DESTINATION, MANIFEST, SOURCE, apply_export, export_tree,
                             load_policy, plan_export, public_snapshot, read_tree, validate_public)


def run(root, *args, auth=None):
    env = os.environ.copy()
    env["GIT_TERMINAL_PROMPT"] = "0"
    # Avoid accidentally forwarding the source checkout's credential to another repository.
    env.update({"GIT_CONFIG_COUNT": "1", "GIT_CONFIG_KEY_0": "credential.helper", "GIT_CONFIG_VALUE_0": ""})
    if auth:
        env.update({"GIT_CONFIG_COUNT": "2", "GIT_CONFIG_KEY_1": "http.https://github.com/.extraheader",
                    "GIT_CONFIG_VALUE_1": "AUTHORIZATION: basic " + base64.b64encode(("x-access-token:" + auth).encode()).decode()})
    proc = subprocess.run(["git", "-c", "core.hooksPath=", "-C", str(root), *args],
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env)
    if proc.returncode:
        raise ValueError("GitHub synchronization operation failed; review permissions, branch rules and target state.")
    return proc.stdout.decode().strip()


def require_hosted_workflow():
    workflow = os.environ.get("GITHUB_WORKFLOW_REF", "")
    if (os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted"
            or os.environ.get("GITHUB_REPOSITORY") != SOURCE
            or os.environ.get("GITHUB_EVENT_NAME") != "push"
            or os.environ.get("GITHUB_REF") != "refs/heads/main"
            or workflow != SOURCE + "/.github/workflows/sync-youriptv.yml@refs/heads/main"):
        raise ValueError("This writer runs only in the authorized main-branch hosted workflow.")


def main():
    try:
        require_hosted_workflow()
        source = Path(os.environ["GITHUB_WORKSPACE"]) / "master"
        destination = Path(os.environ["GITHUB_WORKSPACE"]) / "customer"
        source_token = os.environ["SOURCE_READ_TOKEN"]
        destination_token = os.environ["DESTINATION_WRITE_TOKEN"]
        if not source_token or not destination_token:
            raise ValueError("Scoped synchronization credentials are missing.")
        commit, tree = read_tree(source, os.environ["GITHUB_SHA"])
        if not re.fullmatch(r"[0-9a-f]{40}", commit):
            raise ValueError("Unexpected source commit.")
        latest = run(source, "ls-remote", f"https://github.com/{SOURCE}.git", "refs/heads/main", auth=source_token).split()[0]
        if latest != commit:
            print("Skipped superseded source revision; the latest main push owns synchronization.")
            return 0
        terms = load_restricted_terms()
        validate_public(public_snapshot(tree), terms)
        policy = load_policy(tree)
        payload = export_tree(tree, policy, terms)
        if run(destination, "remote", "get-url", "origin").removesuffix(".git") != f"https://github.com/{DESTINATION}":
            raise ValueError("Unexpected destination remote.")
        if run(destination, "branch", "--show-current") != "main":
            raise ValueError("Destination checkout must start on main.")
        plan = plan_export(destination, commit, payload, policy)
        _, old_tree = read_tree(destination, plan["destinationCommit"])
        candidate = {name: item for name, item in old_tree.items() if name not in plan["deletions"]}
        candidate.update(payload)
        validate_public(public_snapshot(candidate), terms)
        if not plan["changed"]:
            print("Customer source already matches; no synchronization commit created.")
            return 0
        branch = policy["bootstrapBranch"] if plan["bootstrap"] else "main"
        if plan["bootstrap"]:
            if run(destination, "ls-remote", "--heads", "origin", "refs/heads/" + branch, auth=destination_token):
                raise ValueError("Initial synchronization branch already exists. Review/merge it before the next run.")
            run(destination, "switch", "--create", branch)
        apply_export(destination, payload, plan)
        names = plan["updates"] + plan["deletions"] + [MANIFEST]
        for offset in range(0, len(names), 80):
            run(destination, "--literal-pathspecs", "add", "--", *names[offset:offset + 80])
        if not run(destination, "diff", "--cached", "--name-only"):
            print("No source changes; no synchronization commit created.")
            return 0
        run(destination, "-c", "user.name=YourIPTV Source Sync", "-c", "user.email=source-sync@users.noreply.github.com",
            "commit", "-m", "Synchronize approved master source " + commit[:12])
        # Recheck both main tips immediately before a normal fast-forward-only push.
        latest = run(source, "ls-remote", f"https://github.com/{SOURCE}.git", "refs/heads/main", auth=source_token).split()[0]
        target = run(destination, "ls-remote", "origin", "refs/heads/main", auth=destination_token).split()[0]
        if latest != commit or target != plan["destinationCommit"]:
            raise ValueError("A branch changed during validation; retry against the new revision.")
        run(destination, "push", "origin", "HEAD:refs/heads/" + branch, auth=destination_token)
        outcome = "Bootstrap branch prepared. Review and merge it into customer main to activate routine sync." if plan["bootstrap"] else "Customer main synchronized."
        print(outcome)
        summary = os.environ.get("GITHUB_STEP_SUMMARY")
        if summary:
            with open(summary, "a", encoding="utf-8") as stream:
                stream.write(outcome + "\n\nMaster commit: `" + commit + "`\n\n")
                stream.write(f"Updated {len(plan['updates'])} files; removed {len(plan['deletions'])} previously managed files.\n\n")
                if plan["bootstrap"]:
                    stream.write(f"[Review initial source synchronization](https://github.com/{DESTINATION}/compare/main...{branch})\n")
        return 0
    except (ValueError, KeyError, OSError):
        # Do not print exception content: subprocess/environment errors could include credentials.
        print("Synchronization stopped safely. Inspect scoped credentials, reviewed baseline, source audit and destination drift.", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
