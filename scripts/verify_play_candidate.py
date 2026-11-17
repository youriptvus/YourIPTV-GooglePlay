"""Inspect the unified Play AAB/APK and emit reproducible, secret-redacted JSON evidence.

Requires official Android bundletool and aapt2. This is a local verifier; it does
not access GitHub, Google Play, devices, signing keys, or the customer repository.
Pattern-based secret checks cannot prove that arbitrary unknown credentials are absent.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import sys
from datetime import datetime, timezone
import xml.etree.ElementTree as ET
import zipfile


ANDROID = "{http://schemas.android.com/apk/res/android}"
EXCLUDED_DIRECTORIES = {".git", ".gradle", ".kotlin", "build", "node_modules", "__pycache__"}
TEXT_EXTENSIONS = {".kt", ".java", ".xml", ".md", ".py", ".json", ".kts", ".properties", ".yml", ".yaml", ".txt", ".pro"}
SECRET_PATTERNS = {
    "private_key": re.compile(rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "github_token": re.compile(rb"\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,})\b"),
    "aws_access_key": re.compile(rb"\bAKIA[A-Z0-9]{16}\b"),
    "google_api_key": re.compile(rb"\bAIza[0-9A-Za-z_-]{35}\b"),
}
FORBIDDEN_DEX_TOKENS = (
    b"Lcom/youriptv/app/updates/AppUpdates;",
    b"Lcom/youriptv/app/updates/AppUpdateChecks;",
    b"Lcom/youriptv/app/ui/AppUpdateActivity;",
    b"Landroid/content/pm/PackageInstaller;",
    b"android.intent.action.INSTALL_PACKAGE",
    b"android.settings.MANAGE_UNKNOWN_APP_SOURCES",
    b"application/vnd.android.package-archive",
    b"canRequestPackageInstalls",
    b"github.com/youriptvus/YourIPTV/releases/latest/download/update.json",
)
OFFICIAL_CERTIFICATE_SHA256 = "4aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def contains_private_token(data: bytes, token: bytes) -> bool:
    # Short numeric PINs can appear incidentally inside file hashes and timestamps.
    if token.isdigit():
        return re.search(rb"(?<![A-Za-z0-9])" + re.escape(token) + rb"(?![A-Za-z0-9])", data) is not None
    return token in data


def file_digest(path: Path) -> dict:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return {"path": str(path.resolve()), "size_bytes": path.stat().st_size, "sha256": digest.hexdigest()}


def run(command: list[str], *, timeout: int = 180) -> dict:
    try:
        result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return {"command": command, "exit_code": result.returncode, "stdout": result.stdout, "stderr": result.stderr}
    except (OSError, subprocess.TimeoutExpired) as error:
        return {"command": command, "exit_code": None, "stdout": "", "stderr": str(error)}


def truth(value: str | None) -> bool:
    return value is not None and value.lower() not in {"false", "0", "0x0", "0x00000000", ""}


def integer(value: str | None) -> int | None:
    if value is None:
        return None
    try:
        return int(value, 16 if value.lower().startswith("0x") else 10)
    except ValueError:
        return None


def aapt_value(value: str) -> str:
    raw = re.search(r'\(Raw:\s*"(.*)"\)\s*$', value)
    if raw:
        return raw.group(1)
    quoted = re.match(r'^"(.*?)"', value)
    if quoted:
        return quoted.group(1)
    typed = re.search(r"\(type\s+0x([0-9a-fA-F]+)\)\s*(0x[0-9a-fA-F]+)", value)
    if typed:
        type_code, number = int(typed.group(1), 16), int(typed.group(2), 16)
        if type_code == 0x12:
            return "true" if number else "false"
        if type_code == 0x01:
            return f"@0x{number:08x}"
        return str(number)
    return value.strip().split(" ", 1)[0]


def parse_aapt_xml(dump: str) -> ET.Element:
    """Recover an actual binary XML tree from official aapt2 xmltree output."""
    stack: list[tuple[int, ET.Element]] = []
    root = None
    for line in dump.splitlines():
        element = re.match(r"^(\s*)E:\s+([^\s(]+)", line)
        if element:
            indent, name = len(element.group(1)), element.group(2)
            while stack and stack[-1][0] >= indent:
                stack.pop()
            node = ET.Element(name)
            if stack:
                stack[-1][1].append(node)
            elif root is None:
                root = node
            stack.append((indent, node))
            continue
        attribute = re.match(r"^\s*A:\s+([^\s(=]+)(?:\(0x[0-9a-fA-F]+\))?=(.*)$", line)
        if attribute and stack:
            name = attribute.group(1)
            if name.startswith("android:"):
                name = ANDROID + name.removeprefix("android:")
            elif name.startswith("http://schemas.android.com/apk/res/android:"):
                # Build Tools 36 xmltree emits the expanded namespace URI.
                name = ANDROID + name.removeprefix("http://schemas.android.com/apk/res/android:")
            stack[-1][1].set(name, aapt_value(attribute.group(2)))
    if root is None:
        raise ValueError("aapt2 did not return an XML tree")
    return root


def parse_aapt_manifest(dump: str) -> ET.Element:
    root = parse_aapt_xml(dump)
    if root.tag != "manifest":
        raise ValueError("aapt2 did not return an Android manifest tree")
    return root


def inspect_manifest(root: ET.Element, args: argparse.Namespace) -> dict:
    app = root.find("application")
    sdk = root.find("uses-sdk")
    if app is None or sdk is None:
        raise ValueError("Application or uses-sdk declaration missing")
    permissions = sorted({node.get(ANDROID + "name", "") for tag in ("uses-permission", "uses-permission-sdk-23") for node in root.findall(tag)})
    allowed = {
        "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.WAKE_LOCK",
        args.application_id + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
    }
    features = {node.get(ANDROID + "name", ""): not (node.get(ANDROID + "required", "true").lower() == "false") for node in root.findall("uses-feature")}
    launchers = {"android.intent.category.LAUNCHER": [], "android.intent.category.LEANBACK_LAUNCHER": []}
    exported = []
    for node in app:
        name = node.get(ANDROID + "name", "")
        if truth(node.get(ANDROID + "exported")):
            exported.append({"type": node.tag, "name": name, "permission": node.get(ANDROID + "permission")})
        for intent in node.findall("intent-filter"):
            if not any(action.get(ANDROID + "name") == "android.intent.action.MAIN" for action in intent.findall("action")):
                continue
            for category in intent.findall("category"):
                if category.get(ANDROID + "name") in launchers:
                    launchers[category.get(ANDROID + "name")].append(name)
    phone, tv = launchers.values()
    app_main = {".ui.MainActivity", args.application_id + ".ui.MainActivity"}
    unexpected_exported = [entry for entry in exported if not (
        (entry["type"] == "activity" and entry["name"] in app_main) or
        (entry["type"] == "receiver" and entry["name"] == "androidx.profileinstaller.ProfileInstallReceiver" and entry["permission"] == "android.permission.DUMP")
    )]
    dynamic = args.application_id + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
    declarations = {node.get(ANDROID + "name"): node.get(ANDROID + "protectionLevel") for node in root.findall("permission")}
    checks = {
        "application_id": root.get("package") == args.application_id,
        "version_code": integer(root.get(ANDROID + "versionCode")) == args.version_code,
        "version_name": root.get(ANDROID + "versionName") == args.version_name,
        "minimum_sdk": integer(sdk.get(ANDROID + "minSdkVersion")) == args.min_sdk,
        "target_sdk": integer(sdk.get(ANDROID + "targetSdkVersion")) == args.target_sdk,
        "not_debuggable": not truth(app.get(ANDROID + "debuggable")),
        "android_backup_disabled": app.get(ANDROID + "allowBackup", "true").lower() == "false",
        "explicit_legacy_backup_rules_present": bool(app.get(ANDROID + "fullBackupContent", "").startswith("@")),
        "explicit_device_transfer_rules_present": bool(app.get(ANDROID + "dataExtractionRules", "").startswith("@")),
        "only_approved_permissions": set(permissions) <= allowed,
        "network_permissions_present": {"android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"} <= set(permissions),
        "androidx_self_permission_signature_guarded": dynamic not in permissions or
            declarations.get(dynamic) == "signature" or integer(declarations.get(dynamic)) == 2,
        "touchscreen_optional": features.get("android.hardware.touchscreen") is False,
        "leanback_optional": features.get("android.software.leanback") is False,
        "no_required_hardware_features": not any(features.values()),
        "same_phone_and_tv_launcher": len(phone) == 1 and phone == tv and phone[0] in app_main,
        "tv_banner_present": bool(app.get(ANDROID + "banner")),
        "launcher_icon_present": bool(app.get(ANDROID + "icon")) and bool(app.get(ANDROID + "roundIcon")),
        "only_expected_exported_components": not unexpected_exported,
    }
    return {"checks": checks, "package": root.get("package"), "version_code": integer(root.get(ANDROID + "versionCode")),
            "version_name": root.get(ANDROID + "versionName"), "min_sdk": integer(sdk.get(ANDROID + "minSdkVersion")),
            "target_sdk": integer(sdk.get(ANDROID + "targetSdkVersion")), "permissions": permissions,
            "features_required": features, "launchers": launchers, "exported_components": exported,
            "unexpected_exported_components": unexpected_exported,
            "backup_rule_references": {key: app.get(ANDROID + key) for key in ("fullBackupContent", "dataExtractionRules")}}


BACKUP_DOMAINS = {"root", "file", "database", "sharedpref", "external"}
DEVICE_BACKUP_DOMAINS = {"device_root", "device_file", "device_database", "device_sharedpref"}


def inspect_backup_xml(documents: dict[str, ET.Element]) -> dict:
    def excluded(node: ET.Element | None, domains: set[str]) -> bool:
        if node is None:
            return False
        entries = node.findall("exclude")
        return len(entries) == len(domains) and {(entry.get("domain"), entry.get("path")) for entry in entries} == {(domain, ".") for domain in domains}

    legacy23 = documents["legacy23"]
    legacy24 = documents["legacy24"]
    modern = documents["modern"]
    all_domains = BACKUP_DOMAINS | DEVICE_BACKUP_DOMAINS
    checks = {
        "api23_full_backup_root": legacy23.tag == "full-backup-content",
        "api23_all_domains_excluded": excluded(legacy23, BACKUP_DOMAINS),
        "api24_full_backup_root": legacy24.tag == "full-backup-content",
        "api24_all_domains_excluded": excluded(legacy24, all_domains),
        "api31_extraction_root": modern.tag == "data-extraction-rules",
        "api31_cloud_domains_excluded": excluded(modern.find("cloud-backup"), all_domains),
        "api31_device_transfer_domains_excluded": excluded(modern.find("device-transfer"), all_domains),
        "no_backup_includes": all(not root.findall(".//include") for root in documents.values()),
    }
    return {"checks": checks, "excluded_domains": {key: sorted({entry.get("domain", "") for entry in root.findall(".//exclude")}) for key, root in documents.items()}}


def apk_xml_resource_table(dump: str) -> dict:
    """Resolve optimized APK file names and version-qualified alternatives from aapt2."""
    resources = {}
    current = None
    for line in dump.splitlines():
        if re.match(r"^\s*resource\s+", line):
            current = None
        entry = re.match(r"^\s*resource\s+(0x[0-9a-fA-F]+)\s+(?:[A-Za-z0-9_.]+:)?xml/([a-z0-9_]+)\b", line)
        if entry:
            current = resources.setdefault(entry.group(2), {"id": entry.group(1), "configurations": []})
            continue
        variant = re.match(r"^\s*\(([^)]*)\)\s+\(file\)\s+(\S+)\s+type=XML", line)
        if current is not None and variant:
            current["configurations"].append({"qualifier": variant.group(1), "path": variant.group(2)})
    return resources


def backup_resources(args: argparse.Namespace, evidence: dict) -> None:
    specifications = {
        "legacy23": ("backup_rules", "", "base/res/xml/backup_rules.xml"),
        "legacy24": ("backup_rules", "v24", "base/res/xml-v24/backup_rules.xml"),
        "modern": ("data_extraction_rules", "", "base/res/xml/data_extraction_rules.xml"),
    }
    documents = {}
    paths = {}
    for key, (_, _, entry) in specifications.items():
        result = run([str(args.java), "--class-path", str(args.bundletool), str(args.repo_root / "scripts/DumpBundleXml.java"), str(args.aab), entry])
        evidence["commands"].append(result)
        if result["exit_code"] != 0:
            raise ValueError("Cannot decode packaged AAB backup rule: " + key)
        documents[key] = ET.fromstring(result["stdout"])
        paths[key] = entry
    aab = inspect_backup_xml(documents)
    aab["resource_entries"] = paths
    refs = evidence["aab"]["manifest"]["backup_rule_references"]
    aab["checks"]["manifest_full_backup_reference_matches"] = refs["fullBackupContent"] == "@xml/backup_rules"
    aab["checks"]["manifest_extraction_reference_matches"] = refs["dataExtractionRules"] == "@xml/data_extraction_rules"
    evidence["aab"]["backup"] = aab

    table = run([str(args.aapt2), "dump", "resources", str(args.apk)])
    evidence["commands"].append(table)
    if table["exit_code"] != 0:
        raise ValueError("Cannot resolve packaged APK backup resource IDs")
    resources = apk_xml_resource_table(table["stdout"])
    documents, paths = {}, {}
    for key, (name, qualifier, _) in specifications.items():
        variants = resources.get(name, {}).get("configurations", [])
        matches = [variant["path"] for variant in variants if variant["qualifier"] == qualifier]
        if len(matches) != 1:
            raise ValueError("Cannot resolve exactly one packaged APK backup configuration: " + key)
        paths[key] = matches[0]
        result = run([str(args.aapt2), "dump", "xmltree", str(args.apk), "--file", matches[0]])
        evidence["commands"].append(result)
        if result["exit_code"] != 0:
            raise ValueError("Cannot decode packaged APK backup rule: " + key)
        documents[key] = parse_aapt_xml(result["stdout"])
    apk = inspect_backup_xml(documents)
    apk["resource_entries"] = paths
    refs = evidence["apk"]["manifest"]["backup_rule_references"]
    for attribute, name, check in (("fullBackupContent", "backup_rules", "manifest_full_backup_reference_matches"),
                                   ("dataExtractionRules", "data_extraction_rules", "manifest_extraction_reference_matches")):
        reference = refs.get(attribute) or ""
        apk["checks"][check] = reference == "@xml/" + name or (reference.startswith("@0x") and
            integer(reference[1:]) == integer(resources.get(name, {}).get("id")))
    evidence["apk"]["backup"] = apk


def inspect_archive(path: Path, external_tokens: list[bytes]) -> dict:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        native = sorted(name for name in names if name.endswith(".so"))
        signer_files = sorted(name for name in names if re.search(r"(?:^|/)META-INF/[^/]+\.(?:RSA|DSA|EC)$", name, re.I))
        dex_hits, secrets, external_hits, adaptive_icons, banner_images = [], [], [], [], []
        for name in names:
            if name.endswith("/"):
                continue
            if name.endswith(".png"):
                header = archive.read(name)[:24]
                if header.startswith(b"\x89PNG\r\n\x1a\n") and len(header) >= 24:
                    width, height = struct.unpack(">II", header[16:24])
                    if width * 9 == height * 16:
                        banner_images.append({"entry": name, "width": width, "height": height})
            if not (name.endswith(".dex") or Path(name).suffix.lower() in TEXT_EXTENSIONS):
                continue
            data = archive.read(name)
            if name.endswith(".xml") and (b"adaptive-icon" in data or "adaptive-icon".encode("utf-16le") in data):
                adaptive_icons.append(name)
            if name.endswith(".dex"):
                for token in FORBIDDEN_DEX_TOKENS:
                    if token in data:
                        dex_hits.append({"entry": name, "identifier": token.decode("ascii")})
            for category, pattern in SECRET_PATTERNS.items():
                if pattern.search(data):
                    secrets.append({"entry": name, "category": category})
            for index, token in enumerate(external_tokens):
                if contains_private_token(data, token):
                    external_hits.append({"entry": name, "token_index": index})
        central_directory = archive.start_dir
    with path.open("rb") as source:
        source.seek(max(0, central_directory - 16))
        signing_block = source.read(16) == b"APK Sig Block 42"
    present = bool(signer_files or signing_block)
    return {"checks": {"no_native_libraries": not native, "no_apk_installer_identifiers": not dex_hits,
                       "adaptive_icon_resources_present": bool(adaptive_icons),
                       "no_known_secret_patterns": not secrets, "no_external_forbidden_plaintext": not external_hits},
            "native_libraries": native, "installer_identifier_hits": dex_hits, "secret_pattern_hits": secrets,
            "external_forbidden_plaintext_hits": external_hits, "jar_signer_entries": signer_files,
            "adaptive_icon_resources": adaptive_icons, "sixteen_by_nine_png_resources": banner_images,
            "apk_signing_block_present": signing_block, "signing_status": "present_unverified" if present else "unsigned"}


def source_files(repo: Path) -> list[Path]:
    result = run(["git", "--no-optional-locks", "-C", str(repo), "ls-files", "--cached", "--others", "--exclude-standard", "-z"])
    if result["exit_code"] != 0:
        raise ValueError("Cannot obtain independent repository file inventory")
    return sorted({repo / name for name in result["stdout"].split("\0") if name and (repo / name).is_file()})


def inspect_source(repo: Path, inventory: Path | None, external_tokens: list[bytes]) -> dict:
    paths = source_files(repo)
    hits, forbidden_files, external_hits = [], [], []
    production = []
    current = {}
    for path in paths:
        name = path.relative_to(repo).as_posix()
        if any(part in EXCLUDED_DIRECTORIES for part in path.relative_to(repo).parts):
            continue
        data = path.read_bytes()
        current[name] = {"sha256": sha256(data), "normalized_sha256": sha256(data.replace(b"\r\n", b"\n")) if path.suffix.lower() in TEXT_EXTENSIONS else sha256(data)}
        if path.name in {"keystore.properties", "local.properties", "test-config.local.properties"} or path.suffix.lower() in {".jks", ".keystore", ".p12", ".pfx"}:
            forbidden_files.append(name)
        for category, pattern in SECRET_PATTERNS.items():
            if pattern.search(data):
                hits.append({"path": name, "category": category})
        for index, token in enumerate(external_tokens):
            if contains_private_token(data, token):
                external_hits.append({"path": name, "token_index": index})
        if name.startswith("app/src/main/") and path.suffix.lower() in {".kt", ".java", ".xml"}:
            production.append(data)
    code = b"\n".join(production)
    installer_hits = [token.decode("ascii") for token in FORBIDDEN_DEX_TOKENS if token in code]
    result = {"checks": {"no_known_secret_patterns": not hits, "no_private_configuration_files": not forbidden_files,
                          "no_external_forbidden_plaintext": not external_hits, "no_production_installer_identifiers": not installer_hits},
              "file_count": len(current), "secret_pattern_hits": hits, "private_configuration_files": forbidden_files,
              "external_forbidden_plaintext_hits": external_hits, "production_installer_identifier_hits": installer_hits}
    if inventory:
        baseline = json.loads(inventory.read_text(encoding="utf-8"))
        expected = baseline["files"]
        common = current.keys() & expected.keys()
        unchanged = sorted(name for name in common if current[name]["normalized_sha256"] == expected[name]["normalized_sha256"])
        result["baseline_comparison"] = {"source_commit": baseline["source_commit"], "baseline_file_count": len(expected),
                                          "unchanged": unchanged, "changed": sorted(common - set(unchanged)),
                                          "removed": sorted(expected.keys() - current.keys()), "added": sorted(current.keys() - expected.keys())}
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--aab", type=Path, required=True)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--bundletool", type=Path, required=True)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--aapt2", type=Path, required=True)
    parser.add_argument("--apksigner", type=Path)
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--baseline-inventory", type=Path)
    parser.add_argument("--forbidden-plaintext-file", type=Path, help="Optional external UTF-8 token file. Values are never included in evidence.")
    parser.add_argument("--application-id", default="com.youriptv.app")
    parser.add_argument("--version-code", type=int, default=45)
    parser.add_argument("--version-name", default="1.0.9")
    parser.add_argument("--min-sdk", type=int, default=23)
    parser.add_argument("--target-sdk", type=int, default=36)
    parser.add_argument("--require-signed", action="store_true", help="Fail unless both artifact signatures verify; otherwise unsigned is reported accurately.")
    parser.add_argument("--expected-certificate-sha256", default=OFFICIAL_CERTIFICATE_SHA256,
                        help="Expected current APK/AAB signer; change only for an authorized signing plan.")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    expected_certificate = args.expected_certificate_sha256.lower().replace(":", "")
    if not re.fullmatch(r"[0-9a-f]{64}", expected_certificate):
        parser.error("Expected certificate SHA-256 must contain exactly 64 hexadecimal digits")
    evidence = {"schema_version": 1, "generated_utc": datetime.now(timezone.utc).isoformat(),
                "scope": "Static local candidate verification; no Play Console history, Play approval, device, playback or unknown-secret guarantee.",
                "expected": {key: getattr(args, key) for key in ("application_id", "version_code", "version_name", "min_sdk", "target_sdk")},
                "expected_certificate_sha256": expected_certificate,
                "commands": [], "errors": []}
    tokens = []
    if args.forbidden_plaintext_file:
        private_path = args.forbidden_plaintext_file.resolve()
        if private_path.is_relative_to(args.repo_root.resolve()):
            parser.error("Forbidden plaintext token file must remain outside the repository")
        tokens = [line.encode("utf-8") for line in private_path.read_text(encoding="utf-8").splitlines() if line]
    for label, artifact in (("aab", args.aab), ("apk", args.apk)):
        try:
            evidence[label] = file_digest(artifact) | {"archive": inspect_archive(artifact, tokens)}
        except (OSError, zipfile.BadZipFile) as error:
            evidence["errors"].append(f"{label}: {type(error).__name__}: {error}")
    validate = run([str(args.java), "-jar", str(args.bundletool), "validate", "--bundle=" + str(args.aab)])
    evidence["commands"].append(validate)
    evidence["bundletool_valid"] = validate["exit_code"] == 0
    dumped = run([str(args.java), "-jar", str(args.bundletool), "dump", "manifest", "--bundle=" + str(args.aab), "--module=base"])
    evidence["commands"].append(dumped)
    try:
        if dumped["exit_code"] != 0:
            raise ValueError("bundletool manifest command failed")
        evidence["aab"]["manifest"] = inspect_manifest(ET.fromstring(dumped["stdout"]), args)
    except (ValueError, KeyError, ET.ParseError) as error:
        evidence["errors"].append("aab manifest: " + str(error))
    dumped = run([str(args.aapt2), "dump", "xmltree", str(args.apk), "--file", "AndroidManifest.xml"])
    evidence["commands"].append(dumped)
    try:
        if dumped["exit_code"] != 0:
            raise ValueError("aapt2 manifest command failed")
        evidence["apk"]["manifest"] = inspect_manifest(parse_aapt_manifest(dumped["stdout"]), args)
    except (ValueError, KeyError) as error:
        evidence["errors"].append("apk manifest: " + str(error))
    try:
        backup_resources(args, evidence)
    except (OSError, ValueError, KeyError, ET.ParseError, zipfile.BadZipFile) as error:
        evidence["errors"].append("packaged backup rules: " + str(error))
    if args.apksigner:
        signed = run([str(args.apksigner), "verify", "--verbose", "--print-certs", str(args.apk)])
        evidence["commands"].append(signed)
        evidence.setdefault("apk", {})["signature_verified"] = signed["exit_code"] == 0
        certificates = re.findall(r"Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]{64})", signed["stdout"])
        evidence["apk"]["certificate_sha256"] = [certificate.lower() for certificate in certificates]
        evidence["apk"]["expected_certificate_verified"] = len(certificates) == 1 and certificates[0].lower() == expected_certificate
    sibling = args.java.parent / ("jarsigner.exe" if os.name == "nt" else "jarsigner")
    if sibling.exists():
        signed = run([str(sibling), "-verify", "-certs", str(args.aab)])
        evidence["commands"].append(signed)
        evidence.setdefault("aab", {})["signature_verified"] = signed["exit_code"] == 0 and "jar verified." in signed["stdout"].lower()
    keytool = args.java.parent / ("keytool.exe" if os.name == "nt" else "keytool")
    if keytool.exists():
        signed = run([str(keytool), "-printcert", "-jarfile", str(args.aab)])
        evidence["commands"].append(signed)
        certificates = re.findall(r"SHA256:\s*([0-9a-fA-F:]+)", signed["stdout"])
        normalized = [certificate.lower().replace(":", "") for certificate in certificates]
        evidence.setdefault("aab", {})["certificate_sha256"] = normalized
        evidence["aab"]["expected_certificate_verified"] = signed["exit_code"] == 0 and normalized == [expected_certificate]
    try:
        evidence["source"] = inspect_source(args.repo_root.resolve(), args.baseline_inventory, tokens)
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        evidence["errors"].append("source: " + str(error))
    all_checks = {"bundletool_valid": evidence["bundletool_valid"]}
    for label in ("aab", "apk"):
        item = evidence.get(label, {})
        for section in ("archive", "manifest", "backup"):
            for name, passed in item.get(section, {}).get("checks", {}).items():
                all_checks[f"{label}.{section}.{name}"] = passed
        if args.require_signed:
            all_checks[label + ".signature_verified"] = item.get("signature_verified", False)
            all_checks[label + ".expected_certificate_verified"] = item.get("expected_certificate_verified", False)
    for name, passed in evidence.get("source", {}).get("checks", {}).items():
        all_checks["source." + name] = passed
    evidence["failed_checks"] = sorted(name for name, passed in all_checks.items() if not passed)
    evidence["passed"] = not evidence["errors"] and not evidence["failed_checks"]
    evidence["signature_requirement"] = "both_verified" if args.require_signed else "unsigned_allowed_and_reported"
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(evidence, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"passed": evidence["passed"], "failed_checks": evidence["failed_checks"], "errors": evidence["errors"], "evidence": str(args.output.resolve())}))
    return 0 if evidence["passed"] else 1


if __name__ == "__main__":
    sys.exit(main())
