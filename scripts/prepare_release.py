"""Prepare a future, owner-authorized local release pair; never publish anything.

The protected published baseline cannot be rebuilt by this entry point. Plan and
verify do not build. Prepare stages both signed distributions, verifies them and
activates a previously absent version directory only after every gate passes.
"""
from __future__ import annotations

import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import uuid
from urllib.parse import urlsplit
import zipfile

import public_guard
import verify_play_candidate as play_audit

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = Path(r"D:\APK\Releases\YourIPTV")
DEFAULT_CONFIG = Path(__file__).with_name("release_config.json")
SIGNING_ENV = {"play": "YOURIPTV_UPLOAD_SIGNING_PROPERTIES",
               "customer": "YOURIPTV_CUSTOMER_SIGNING_PROPERTIES"}
VERSION_PATTERN = re.compile(r"[0-9A-Za-z][0-9A-Za-z.+-]{0,31}\Z")
HEX_PATTERN = re.compile(r"[0-9a-f]{64}\Z")
MAX_APK_SIZE = 100 * 1024 * 1024


class ReleaseError(ValueError):
    """An actionable gate failure whose text never contains private input."""


def require(condition, message):
    if not condition:
        raise ReleaseError(message)


def digest(path):
    checksum = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            checksum.update(block)
    return {"size": path.stat().st_size, "sha256": checksum.hexdigest()}


def properties(path):
    """Read the deliberately simple public version/default properties files."""
    values = {}
    for row in path.read_text(encoding="utf-8-sig").splitlines():
        row = row.strip()
        if row and not row.startswith(("#", "!")):
            key, separator, value = row.partition("=")
            require(bool(separator) and key.strip() not in values, "Invalid or duplicate property.")
            values[key.strip()] = value.strip()
    return values


def configuration(path):
    config = json.loads(path.read_text(encoding="utf-8"))
    require(config.get("schemaVersion") == 1, "Unsupported release configuration.")
    require(config.get("applicationId") == "com.youriptv.app", "Official application identity changed.")
    for key in ("playUploadCertificateSha256", "customerCertificateSha256"):
        require(bool(HEX_PATTERN.fullmatch(config.get(key, ""))), "Invalid public certificate pin.")
    require(config.get("protectedVersionName") == "1.1.1" and
            config.get("protectedVersionCode") == 52, "Published baseline protection changed.")
    require(config.get("playMinSdk") == 24 and config.get("customerMinSdk") == 23 and
            config.get("targetSdk") == 36, "SDK policy requires an explicit implementation review.")
    return config


def validate_identity(version, code, highest_play, previous_customer, confirmed, shared, config):
    require(bool(VERSION_PATTERN.fullmatch(version)) and version not in (".", ".."), "Invalid version name.")
    require(type(code) is int and 0 < code <= 2100000000, "Invalid Android version code.")
    require(version != config["protectedVersionName"] and code > config["protectedVersionCode"],
            "Published 1.1.1/code52 is protected. Request a future release explicitly.")
    require(shared.get("versionName") == version and shared.get("versionCode") == str(code),
            "Requested release must match committed version.properties.")
    require(confirmed, "Confirm the latest uploaded Play version history explicitly.")
    require(type(highest_play) is int and highest_play >= config["protectedVersionCode"] and code > highest_play,
            "Version code must exceed the confirmed highest Play upload.")
    require(type(previous_customer) is int and 0 <= previous_customer < code,
            "Previous customer code must be a lower published code; it is not a minimum supported client.")


def canonical_customer_url(value, version):
    allowed = {
        f"https://github.com/youriptvus/YourIPTV/releases/download/v{version}/YourIPTV.apk",
        f"https://youriptv.us/updates/YourIPTV-{version}-release.apk",
    }
    require(value in allowed and not urlsplit(value).query, "Supply an exact owner-approved canonical customer APK URL.")
    return value


def command(arguments, *, cwd=None, env=None, timeout=180):
    """Never echo process output: signing/tool diagnostics may contain private values."""
    try:
        result = subprocess.run([str(x) for x in arguments], cwd=cwd, env=env,
                                capture_output=True, timeout=timeout, check=False)
    except (OSError, subprocess.TimeoutExpired):
        raise ReleaseError("A required local tool failed to start or timed out.") from None
    require(result.returncode == 0, "A required local check failed; no release directory was activated.")
    return result.stdout.decode("utf-8", errors="replace")


def source_provenance(root, terms):
    require(not command(["git", "status", "--porcelain", "--untracked-files=normal"], cwd=root).strip(),
            "Commit and review all source changes locally before release preparation.")
    commit = command(["git", "rev-parse", "HEAD"], cwd=root).strip()
    require(bool(re.fullmatch(r"[0-9a-f]{40,64}", commit)), "Invalid source commit identity.")
    names = command(["git", "ls-files", "-z"], cwd=root).split("\0")
    files = {}
    for name in filter(None, names):
        candidate = root / name
        require(not candidate.is_symlink() and candidate.resolve().is_relative_to(root.resolve()) and
                candidate.is_file(), "Source contains a missing, linked or unsafe file.")
        files[name] = candidate.read_bytes()
    audit = public_guard.audit_files(files, terms)
    require(audit["passed"], "Source publication audit failed; inspect the private guard report separately.")
    return {"sourceCommit": commit, "sourceFiles": len(files), "sourceAuditPassed": True}


def release_plan(args, config, *, root=ROOT, provenance=source_provenance):
    validate_identity(args.version, args.version_code, args.highest_play_code,
                      args.previous_customer_code, args.play_history_confirmed,
                      properties(root / "version.properties"), config)
    terms = public_guard.load_restricted_terms(args.restricted_terms_file)
    origin = provenance(root, terms)
    output = args.output_root.resolve()
    require(not output.is_relative_to(root.resolve()), "Release output must remain outside the source repository.")
    destination = output / args.version
    require(not destination.exists(), "Version directory already exists; immutable releases cannot be overwritten.")
    notes = args.release_notes.read_text(encoding="utf-8-sig").strip()
    require(bool(notes) and len(notes) <= 16000, "Supply nonempty release notes of at most 16000 characters.")
    require(public_guard.audit_files({"release-notes.txt": notes.encode()}, terms)["passed"],
            "Release notes failed the publication audit.")
    customer_url = canonical_customer_url(args.customer_apk_url, args.version)
    result = {"schemaVersion": 1, "versionName": args.version, "versionCode": args.version_code,
              "highestPlayCodeConfirmed": args.highest_play_code,
              "previousCustomerCode": args.previous_customer_code, **origin,
              "outputDirectory": str(destination), "applicationId": config["applicationId"],
              "customerApkUrl": customer_url, "releaseNotes": notes,
              "publication": "manual; no network publishing is implemented",
              "toolchain": config["toolchain"],
              "buildCommands": build_commands(root)}
    return result, terms


def build_commands(root=ROOT):
    wrapper = str(root / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    common = [wrapper, "--no-daemon", "--max-workers=1", "--console=plain",
              "-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8",
              "-Pkotlin.compiler.execution.strategy=in-process"]
    return {"play": common + ["-Pdistribution=play", "testReleaseUnitTest", "lintRelease",
                               "assembleRelease", "bundleRelease"],
            "customer": common + ["-Pdistribution=customer", "testReleaseUnitTest", "lintRelease", "assembleRelease"]}


def toolchain(args, config):
    java_home = os.environ.get("JAVA_HOME", "")
    java = args.java or (Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java"))
    sdk_value = args.sdk or os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
    require(bool(sdk_value), "Supply --sdk or ANDROID_SDK_ROOT.")
    sdk = Path(sdk_value)
    build_tools = sdk / "build-tools" / config["toolchain"]["buildTools"]
    suffix = ".exe" if os.name == "nt" else ""
    bundletool_value = args.bundletool or os.environ.get("YOURIPTV_BUNDLETOOL")
    require(bool(bundletool_value), "Supply --bundletool or YOURIPTV_BUNDLETOOL.")
    result = {"java": Path(java), "aapt2": build_tools / ("aapt2" + suffix),
              "apksignerJar": build_tools / "lib" / "apksigner.jar",
              "bundletool": Path(bundletool_value),
              "keytool": Path(java).parent / ("keytool" + suffix),
              "jarsigner": Path(java).parent / ("jarsigner" + suffix)}
    require(all(value.is_file() for value in result.values()), "Required local Android/JDK/bundletool files are missing.")
    version = command([result["java"], "-XshowSettings:properties", "-version"])
    # Java writes version properties to stderr. Query through a dedicated safe capture.
    probe = subprocess.run([str(result["java"]), "-version"], capture_output=True, check=False)
    version += (probe.stdout + probe.stderr).decode("utf-8", errors="replace")
    require(probe.returncode == 0 and bool(re.search(r'version "21(?:[.\"]|$)', version)), "Release builds require the pinned JDK21.")
    return result


def signing_inputs(args, root=ROOT):
    result = {}
    for distribution, option in (("play", args.play_signing_properties), ("customer", args.customer_signing_properties)):
        value = option or os.environ.get(SIGNING_ENV[distribution])
        require(bool(value), "Both independent external signing configurations are required.")
        path = Path(value).resolve()
        require(path.is_file() and not path.is_relative_to(root.resolve()) and
                not path.is_relative_to(root.parent / "YourIPTV"), "Signing configuration must remain outside both repositories.")
        require(path.stat().st_size <= 65536, "Invalid signing configuration size.")
        # Gradle's java.util.Properties owns parsing and relative key-path resolution.
        # This preflight checks presence only and never reads or logs private values.
        result[distribution] = path
    return result


def artifact_payload_audit(path, terms):
    require(path.is_file() and not path.is_symlink(), "Missing or linked release artifact.")
    payloads = {}
    with zipfile.ZipFile(path) as archive:
        infos = archive.infolist()
        require(len(infos) <= 100000 and sum(item.file_size for item in infos) <= 512 * 1024 * 1024,
                "Release archive exceeds inspection limits.")
        seen = set()
        for index, item in enumerate(infos):
            name = item.filename.replace("\\", "/")
            require(not name.startswith("/") and ".." not in name.split("/") and name.casefold() not in seen,
                    "Release archive contains unsafe or duplicate members.")
            seen.add(name.casefold())
            require((item.external_attr >> 16) & 0o170000 != 0o120000, "Release archive contains a linked member.")
            require(not any(term.casefold() in name.casefold() for term in terms), "Release archive contains a restricted member.")
            require(not name.casefold().endswith((".jks", ".keystore", ".p12", ".pfx", ".key", ".pem")) and
                    Path(name).name.casefold() not in {"keystore.properties", "local.properties", "test-config.local.properties"},
                    "Release archive contains private material.")
            if not item.is_dir():
                # Generated ZIP names are checked above; the guard inspects every payload's contents.
                payloads[f"payload/{index}.bin"] = archive.read(item)
    audit = public_guard.audit_files(payloads, terms)
    require(audit["passed"], "Release artifact failed its private-content audit.")
    return {"passed": True, "membersChecked": len(payloads)}


def apk_identity(path, tools, version, code, minimum, config, expected_certificate, *, customer):
    dumped = command([tools["aapt2"], "dump", "xmltree", path, "--file", "AndroidManifest.xml"])
    root = play_audit.parse_aapt_manifest(dumped)
    android = "{http://schemas.android.com/apk/res/android}"
    sdk, app = root.find("uses-sdk"), root.find("application")
    require(sdk is not None and app is not None, "Compiled application or SDK declaration is missing.")
    require(root.get("package") == config["applicationId"] and root.get(android + "versionName") == version and
            play_audit.integer(root.get(android + "versionCode")) == code and
            play_audit.integer(sdk.get(android + "minSdkVersion")) == minimum and
            play_audit.integer(sdk.get(android + "targetSdkVersion")) == config["targetSdk"] and
            not play_audit.truth(app.get(android + "debuggable")), "Compiled release identity or SDK policy mismatch.")
    permissions = {node.get(android + "name") for tag in ("uses-permission", "uses-permission-sdk-23") for node in root.findall(tag)}
    if customer:
        required = set(config["customerPermissions"]) - {
            "android.permission.WAKE_LOCK", config["applicationId"] + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"}
        require(required <= permissions <= set(config["customerPermissions"]), "Customer permissions changed.")
    signature = command([tools["java"], "-jar", tools["apksignerJar"], "verify", "--print-certs", path])
    certificates = re.findall(r"Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]{64})", signature)
    require([value.lower() for value in certificates] == [expected_certificate], "APK signing certificate mismatch.")
    return {"applicationId": config["applicationId"], "versionName": version, "versionCode": code,
            "minSdk": minimum, "targetSdk": config["targetSdk"], "certificateSha256": expected_certificate,
            "permissions": sorted(permissions), "signatureVerified": True}


def verify_pair(directory, config, tools, terms, *, expected=None, root=ROOT):
    records, audits = {}, {}
    for distribution, folder, suffix, minimum, cert_key in (
        ("google-play", "GooglePlay", "aab", config["playMinSdk"], "playUploadCertificateSha256"),
        ("customer-github", "GitHub", "apk", config["customerMinSdk"], "customerCertificateSha256")):
        record = json.loads((directory / folder / "release.json").read_text(encoding="utf-8"))
        records[distribution] = record
        require(record.get("schemaVersion") == 1 and record.get("distribution") == distribution and
                record.get("applicationId") == config["applicationId"] and
                record.get("applicationName") == config["applicationName"], "Release record identity mismatch.")
        version, code = record.get("versionName", ""), record.get("versionCode")
        require(bool(VERSION_PATTERN.fullmatch(version)) and type(code) is int and
                version != config["protectedVersionName"] and code > config["protectedVersionCode"],
                "Release verification accepts only future candidates.")
        label = "GooglePlay" if suffix == "aab" else "GitHub"
        require(record.get("artifact") == f"YourIPTV-{label}-{version}.{suffix}", "Artifact name mismatch.")
        require(record.get("minSdk") == minimum and record.get("targetSdk") == config["targetSdk"] and
                record.get("certificateSha256") == config[cert_key], "Release SDK or certificate pin mismatch.")
        require(bool(re.fullmatch(r"[0-9a-f]{40,64}", record.get("sourceCommit", ""))), "Missing source provenance.")
        path = directory / folder / record["artifact"]
        require(all(record.get(key) == value for key, value in digest(path).items()), "Artifact size or SHA-256 mismatch.")
        require(record.get("artifactType") == suffix, "Artifact type mismatch.")
        require(public_guard.audit_files({"release.json": json.dumps(record).encode()}, terms)["passed"],
                "Release metadata failed its publication audit.")
        audits[distribution] = artifact_payload_audit(path, terms)
    play, customer = records["google-play"], records["customer-github"]
    require(all(play.get(key) == customer.get(key) for key in ("versionName", "versionCode", "sourceCommit", "builtUtc", "releaseNotes")),
            "Distributions do not share one release identity and source commit.")
    if expected:
        require(all(play.get(key) == expected.get(key) for key in ("versionName", "versionCode", "sourceCommit")),
                "Built release differs from the approved plan.")
    update = json.loads((directory / "GitHub" / "update.json").read_text(encoding="utf-8"))
    expected_update = updater_record(customer, customer.get("previousVersionCode"), customer.get("downloadUrl"))
    require(update == expected_update, "Legacy updater metadata does not match the customer APK.")
    audits["customerManifest"] = apk_identity(directory / "GitHub" / customer["artifact"], tools,
        customer["versionName"], customer["versionCode"], config["customerMinSdk"], config,
        config["customerCertificateSha256"], customer=True)
    companion = directory / "_verification" / "play-companion.apk"
    artifact_payload_audit(companion, terms)
    # The existing comprehensive Play verifier checks AAB+APK compiled manifests,
    # backup and transport resources, installer exclusion and both signatures.
    with tempfile.TemporaryDirectory(prefix="youriptv-verify-") as temporary:
        temporary = Path(temporary)
        token_file = temporary / "restricted.txt"
        token_file.write_text("\n".join(terms), encoding="utf-8")
        report = temporary / "audit.json"
        apksigner = tools["apksignerJar"].parents[1] / ("apksigner.bat" if os.name == "nt" else "apksigner")
        require(apksigner.is_file(), "SDK apksigner launcher is missing.")
        command([sys.executable, Path(__file__).with_name("verify_play_candidate.py"),
                 "--aab", directory / "GooglePlay" / play["artifact"], "--apk", companion,
                 "--bundletool", tools["bundletool"], "--java", tools["java"], "--aapt2", tools["aapt2"],
                 "--apksigner", apksigner, "--repo-root", root, "--application-id", config["applicationId"],
                 "--version-code", str(play["versionCode"]), "--version-name", play["versionName"],
                 "--min-sdk", str(config["playMinSdk"]), "--target-sdk", str(config["targetSdk"]),
                 "--require-signed", "--expected-certificate-sha256", config["playUploadCertificateSha256"],
                 "--forbidden-plaintext-file", token_file, "--output", report], timeout=600)
        require(json.loads(report.read_text(encoding="utf-8")).get("passed") is True, "Comprehensive Play audit failed.")
    audits["playAuditPassed"] = True
    return {"passed": True, "versionName": play["versionName"], "versionCode": play["versionCode"],
            "sourceCommit": play["sourceCommit"], "checks": audits}


def updater_record(record, previous, url):
    require(type(previous) is int and 0 <= previous < record["versionCode"], "Invalid previous published customer code.")
    require(0 < record["size"] <= MAX_APK_SIZE and bool(HEX_PATTERN.fullmatch(record["sha256"])), "Invalid updater APK identity.")
    canonical_customer_url(url, record["versionName"])
    return {"versionCode": record["versionCode"], "versionName": record["versionName"],
            "apk": url, "size": record["size"], "sha256": record["sha256"], "previousVersionCode": previous}


def artifact_record(path, plan, config, distribution, built_utc):
    customer = distribution == "customer-github"
    record = {"schemaVersion": 1, "applicationName": config["applicationName"], "applicationId": config["applicationId"],
              "versionName": plan["versionName"], "versionCode": plan["versionCode"], "distribution": distribution,
              "artifact": path.name, "artifactType": "apk" if customer else "aab", **digest(path),
              "checksumAlgorithm": "SHA-256", "sourceCommit": plan["sourceCommit"], "builtUtc": built_utc,
              "releaseNotes": plan["releaseNotes"], "minSdk": config["customerMinSdk" if customer else "playMinSdk"],
              "targetSdk": config["targetSdk"],
              "certificateSha256": config["customerCertificateSha256" if customer else "playUploadCertificateSha256"]}
    if customer:
        record.update(downloadUrl=plan["customerApkUrl"], previousVersionCode=plan["previousCustomerCode"],
                      publicArtifactAlias="YourIPTV.apk" if "github.com/" in plan["customerApkUrl"] else f"YourIPTV-{plan['versionName']}-release.apk")
    return record


def write_json(path, value):
    with path.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(value, stream, indent=2, ensure_ascii=False)
        stream.write("\n")


@contextmanager
def exclusive_stage(output, version):
    output.mkdir(parents=True, exist_ok=True)
    lock = output / ("." + version + ".lock")
    descriptor = None
    stage = None
    try:
        descriptor = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        require(not (output / version).exists(), "Version directory already exists; nothing was replaced.")
        stage = Path(tempfile.mkdtemp(prefix=".staging-" + uuid.uuid4().hex + "-", dir=output))
        yield stage
    except FileExistsError:
        raise ReleaseError("A release preparation is already active or its lock requires review.") from None
    finally:
        if stage is not None and stage.exists():
            # Only the newly allocated stage may be removed; never a computed release directory.
            require(stage.resolve().parent == output.resolve() and stage.name.startswith(".staging-"), "Unsafe staging cleanup target.")
            shutil.rmtree(stage)
        if descriptor is not None:
            os.close(descriptor)
            lock.unlink()


def prepare(args, plan, config, tools, terms, *, root=ROOT, runner=command, verifier=verify_pair):
    require(args.authorize_release, "Prepare requires --authorize-release for this future version.")
    signing = signing_inputs(args, root)
    output = args.output_root.resolve()
    with exclusive_stage(output, plan["versionName"]) as stage:
        environment = os.environ.copy()
        environment["JAVA_HOME"] = str(tools["java"].parent.parent)
        if args.sdk:
            environment["ANDROID_SDK_ROOT"] = str(args.sdk.resolve())
        for distribution in ("play", "customer"):
            environment[SIGNING_ENV[distribution]] = str(signing[distribution])
            runner(build_commands(root)[distribution], cwd=root, env=environment, timeout=3600)
        require(source_provenance(root, terms)["sourceCommit"] == plan["sourceCommit"], "Source changed during release preparation.")
        version = plan["versionName"]
        for folder in ("GooglePlay", "GitHub", "_verification"):
            (stage / folder).mkdir()
        artifacts = {
            "google-play": (root / "app/build/play/outputs/bundle/release/app-release.aab", stage / "GooglePlay" / f"YourIPTV-GooglePlay-{version}.aab"),
            "customer-github": (root / "app/build/customer/outputs/apk/release/app-release.apk", stage / "GitHub" / f"YourIPTV-GitHub-{version}.apk")}
        built_utc = datetime.now(timezone.utc).isoformat()
        for distribution, (source, destination) in artifacts.items():
            require(source.is_file() and not source.is_symlink(), "Expected signed build output is missing.")
            shutil.copyfile(source, destination)
            record = artifact_record(destination, plan, config, distribution, built_utc)
            write_json(destination.parent / "release.json", record)
            if distribution == "customer-github":
                write_json(destination.parent / "update.json", updater_record(record, plan["previousCustomerCode"], plan["customerApkUrl"]))
        shutil.copyfile(root / "app/build/play/outputs/apk/release/app-release.apk", stage / "_verification/play-companion.apk")
        result = verifier(stage, config, tools, terms, expected=plan, root=root)
        require(result.get("passed") is True, "Final pair verification failed.")
        write_json(stage / "_verification/verification.json", result)
        write_json(stage / "_verification/build.json", {"toolchain": config["toolchain"], "commands": build_commands(root),
                                                       "sourceCommit": plan["sourceCommit"], "publication": "not performed"})
        # Every local artifact/record is complete before the absent version folder becomes visible.
        require(not (output / version).exists(), "Version directory appeared during preparation; nothing was replaced.")
        stage.rename(output / version)
    return {"passed": True, "releaseDirectory": str(output / version), "publication": "not performed"}


def parser():
    result = argparse.ArgumentParser(description=__doc__)
    commands = result.add_subparsers(dest="operation", required=True)
    for operation in ("plan", "prepare", "verify"):
        sub = commands.add_parser(operation)
        sub.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
        sub.add_argument("--restricted-terms-file", type=Path)
        for name in ("java", "sdk", "bundletool"):
            sub.add_argument("--" + name, type=Path)
        if operation == "verify":
            sub.add_argument("--release-dir", type=Path, required=True)
            continue
        sub.add_argument("--version", required=True)
        sub.add_argument("--version-code", type=int, required=True)
        sub.add_argument("--highest-play-code", type=int, required=True)
        sub.add_argument("--previous-customer-code", type=int, required=True)
        sub.add_argument("--play-history-confirmed", action="store_true")
        sub.add_argument("--release-notes", type=Path, required=True)
        sub.add_argument("--customer-apk-url", required=True)
        sub.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT)
        if operation == "prepare":
            sub.add_argument("--authorize-release", action="store_true")
            sub.add_argument("--play-signing-properties", type=Path)
            sub.add_argument("--customer-signing-properties", type=Path)
    return result


def main(argv=None):
    args = parser().parse_args(argv)
    try:
        config = configuration(args.config)
        if args.operation == "verify":
            terms = public_guard.load_restricted_terms(args.restricted_terms_file)
            result = verify_pair(args.release_dir.resolve(), config, toolchain(args, config), terms)
        else:
            plan, terms = release_plan(args, config)
            result = plan if args.operation == "plan" else prepare(args, plan, config, toolchain(args, config), terms)
        print(json.dumps(result, indent=2))
        return 0
    except (ReleaseError, ValueError, OSError, KeyError, TypeError, zipfile.BadZipFile):
        # Do not include raw exception values, local secret paths, process output or input terms.
        print(json.dumps({"passed": False, "error": "Release preparation gate failed. Check the documented prerequisites; no existing release was replaced."}))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
