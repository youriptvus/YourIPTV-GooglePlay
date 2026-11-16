"""Run the explicitly isolated Android smoke APKs without updating customer installs."""
import argparse
import datetime
import json
from pathlib import Path
import re
import secrets
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'com.youriptv.app.playtest'
TEST_PACKAGE = PACKAGE + '.test'


def run(command, timeout=120):
    return subprocess.check_output([str(arg) for arg in command], text=True,
                                   encoding='utf-8', errors='replace', timeout=timeout).strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--expected-manufacturer', required=True)
    parser.add_argument('--adb', required=True, type=Path)
    parser.add_argument('--aapt', required=True, type=Path)
    parser.add_argument('--apk', type=Path, default=ROOT / 'app/build/outputs/apk/isolatedTest/app-isolatedTest.apk')
    parser.add_argument('--test-apk', type=Path,
                        default=ROOT / 'app/build/outputs/apk/androidTest/isolatedTest/app-isolatedTest-androidTest.apk')
    parser.add_argument('--output', type=Path, default=ROOT / 'app/build/device-evidence')
    parser.add_argument('--screenshots', type=Path, default=ROOT / 'GooglePlay-v1.0.9/screenshots')
    parser.add_argument('--fixture-base', default='http://127.0.0.1:18765')
    parser.add_argument('--soak-seconds', type=int, default=0)
    args = parser.parse_args()
    for apk, expected in [(args.apk, PACKAGE), (args.test_apk, TEST_PACKAGE)]:
        badging = run([args.aapt, 'dump', 'badging', apk])
        actual = re.search(r"^package: name='([^']+)'", badging, re.M)
        if not actual or actual.group(1) != expected:
            raise RuntimeError(f'Refusing to install an APK outside the isolated package: {apk}')
    if args.fixture_base and args.fixture_base != 'http://127.0.0.1:18765':
        raise RuntimeError('Only the local synthetic fixture server is permitted')
    if not 0 <= args.soak_seconds <= 900:
        raise RuntimeError('Bounded observation duration must be between 0 and 900 seconds')

    def adb(*command, timeout=120):
        return run([args.adb, '-s', args.serial, *command], timeout)

    if adb('get-state') != 'device':
        raise RuntimeError('Requested ADB device is not authorized and connected')
    manufacturer = adb('shell', 'getprop', 'ro.product.manufacturer')
    if manufacturer.casefold() != args.expected_manufacturer.casefold():
        raise RuntimeError('Device manufacturer does not match the explicitly selected target')
    identity = {'serial': args.serial, 'manufacturer': manufacturer,
                'model': adb('shell', 'getprop', 'ro.product.model'),
                'sdk': adb('shell', 'getprop', 'ro.build.version.sdk')}

    def customer_state():
        package = adb('shell', 'dumpsys', 'package', 'com.youriptv.app')
        return [line.strip() for line in package.splitlines()
                if any(key in line for key in ['versionCode=', 'versionName=', 'lastUpdateTime=', 'firstInstallTime='])]

    before = customer_state()
    timestamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    evidence = args.output / f'{timestamp}-{args.serial.replace(":", "-")}'
    evidence.mkdir(parents=True, exist_ok=False)
    (evidence / 'identity.json').write_text(json.dumps(identity, indent=2), encoding='utf-8')
    print(f"Verified {identity['manufacturer']} {identity['model']}; installing only {PACKAGE}", flush=True)
    adb('install', '-r', '-t', args.apk)
    adb('install', '-r', '-t', args.test_apk)
    if args.fixture_base:
        adb('reverse', 'tcp:18765', 'tcp:18765')
    try:
        run_id = secrets.token_hex(16)
        command = ['shell', 'am', 'instrument', '-w', '-e', 'runId', run_id,
                   '-e', 'soakSeconds', str(args.soak_seconds)]
        if args.fixture_base:
            command += ['-e', 'fixtureBase', args.fixture_base]
        command += [f'{TEST_PACKAGE}/com.youriptv.app.PlaySmokeInstrumentation']
        output = adb(*command, timeout=300 + args.soak_seconds)
        (evidence / 'instrumentation.txt').write_text(output, encoding='utf-8')
        remote = f'/sdcard/Android/data/{PACKAGE}/files'
        adb('pull', f'{remote}/play-smoke-results.json', evidence / 'play-smoke-results.json')
        report = json.loads((evidence / 'play-smoke-results.json').read_text(encoding='utf-8'))
        if report.get('runId') != run_id:
            raise RuntimeError('Instrumentation did not produce a fresh report for this run')
        args.screenshots.mkdir(parents=True, exist_ok=True)
        screenshots = adb('shell', 'ls', f'{remote}/screenshots').splitlines()
        for filename in screenshots:
            if not re.fullmatch(r'(tv-onn|phone-samsung)-playtest-[a-z-]+\.png', filename):
                raise RuntimeError('Unexpected screenshot filename; refusing arbitrary file copy')
            adb('pull', f'{remote}/screenshots/{filename}', args.screenshots / filename)
        after = customer_state()
        unchanged = before == after
        (evidence / 'customer-installation-preserved.json').write_text(
            json.dumps({'before': before, 'after': after, 'unchanged': unchanged}, indent=2), encoding='utf-8')
        print(json.dumps({'device': identity, 'tests': report['tests'], 'failures': report['failures'],
                          'customer_installation_unchanged': unchanged, 'evidence': str(evidence)}, indent=2), flush=True)
        if not unchanged:
            raise RuntimeError('Customer package installation metadata changed unexpectedly')
        if report['failures'] != 0:
            failed = [row for row in report['results'] if row['status'] == 'failed']
            print(json.dumps(failed, indent=2), flush=True)
            raise SystemExit(1)
    finally:
        if args.fixture_base:
            adb('reverse', '--remove', 'tcp:18765')


if __name__ == '__main__':
    main()
