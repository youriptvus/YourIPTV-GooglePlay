"""Run the explicitly isolated Android smoke APKs without updating customer installs."""
import argparse
import datetime
import json
from pathlib import Path
import re
import secrets
import subprocess
import time
import xml.etree.ElementTree as ET

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
    parser.add_argument('--scope', choices=['full', 'browser-only', 'browser-and-settings'], default='full')
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
    reverse_created = False
    if args.fixture_base:
        routes = adb('reverse', '--list').splitlines()
        if any('tcp:18765' in line.split() for line in routes):
            raise RuntimeError('Refusing to replace an existing ADB reverse route on the fixture port')

    def customer_state():
        package = adb('shell', 'dumpsys', 'package', 'com.youriptv.app')
        return [line.strip() for line in package.splitlines()
                if any(key in line for key in ['versionCode=', 'versionName=', 'lastUpdateTime=', 'firstInstallTime='])]

    def isolated_pid():
        rows = adb('shell', 'ps', '-A').splitlines()
        matches = [line.split() for line in rows if line.split() and line.split()[-1] == PACKAGE]
        return matches[0][1] if matches else None

    def play_policy():
        # Only the actual .playtest preferences are read; their contents are never printed.
        xml = adb('shell', 'run-as', PACKAGE, 'cat', 'shared_prefs/settings.xml')
        tree = ET.fromstring(xml)
        lock = tree.find("boolean[@name='settings_lock_enabled']")
        active = tree.find("long[@name='active_playlist']")
        if lock is None:
            raise RuntimeError('The test package did not persist its local Settings Lock choice')
        return {'settings_lock_enabled': lock.get('value') == 'true',
                'active_playlist_id': active.get('value') if active is not None else None}

    def start_isolated():
        output = adb('shell', 'am', 'start', '-W', '-n', f'{PACKAGE}/com.youriptv.app.ui.MainActivity')
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            process = isolated_pid()
            if process:
                return {'pid': process, 'launch': output}
            time.sleep(0.25)
        raise RuntimeError('The isolated application did not start a process')

    def raw_stage_capture(stage):
        device = 'tv-onn' if manufacturer.casefold() == 'onn' else 'phone-samsung'
        time.sleep(1)
        def require_isolated_foreground():
            activities = adb('shell', 'dumpsys', 'activity', 'activities')
            resumed = [line for line in activities.splitlines()
                       if 'mResumedActivity' in line or 'topResumedActivity' in line]
            if not resumed or not all(re.search(r'(?<![\w.])' + re.escape(PACKAGE) + r'/', line)
                                      for line in resumed):
                raise RuntimeError('Refusing a screenshot when the isolated test app is not the resumed package')
        require_isolated_foreground()
        png = subprocess.check_output([str(args.adb), '-s', args.serial, 'exec-out', 'screencap', '-p'], timeout=30)
        require_isolated_foreground()
        if not png.startswith(b'\x89PNG\r\n\x1a\n'):
            raise RuntimeError('ADB did not return a raw PNG screenshot')
        (args.screenshots / f'{device}-playtest-{stage}.png').write_bytes(png)

    before = customer_state()
    installed = adb('shell', 'pm', 'list', 'packages', '--user', '0', PACKAGE).splitlines()
    isolated_existed = f'package:{PACKAGE}' in installed
    timestamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    evidence = args.output / f'{timestamp}-{args.serial.replace(":", "-")}'
    evidence.mkdir(parents=True, exist_ok=False)
    (evidence / 'identity.json').write_text(json.dumps(identity, indent=2), encoding='utf-8')
    (evidence / 'installation-scope.json').write_text(json.dumps({
        'isolated_package': PACKAGE, 'existing_qa_installation': isolated_existed,
        'fresh_isolated_installation': not isolated_existed,
        'scope': 'A pre-existing QA app is updated without clearing its data. Fresh-context preference checks are not a fresh APK installation.'
    }, indent=2), encoding='utf-8')
    print(f"Verified {identity['manufacturer']} {identity['model']}; installing only {PACKAGE}", flush=True)
    adb('install', '--user', '0', '-r', '-t', args.apk)
    adb('install', '--user', '0', '-r', '-t', args.test_apk)
    if args.fixture_base:
        adb('reverse', 'tcp:18765', 'tcp:18765')
        reverse_created = True
    try:
        run_id = secrets.token_hex(16)
        command = ['shell', 'am', 'instrument', '-w', '-e', 'runId', run_id,
                   '-e', 'soakSeconds', str(args.soak_seconds), '-e', 'scope', args.scope]
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
        # Focused reruns copy only screenshots that their successful cases recaptured.
        screenshots = adb('shell', 'ls', f'{remote}/screenshots').splitlines() if args.scope == 'full' else []
        if args.scope == 'browser-and-settings' and any(
                row['name'] == 'normal Settings UI opens directly without a PIN' and row['status'] == 'passed'
                for row in report['results']):
            device_name = 'tv-onn' if manufacturer.casefold() == 'onn' else 'phone-samsung'
            screenshots = [f'{device_name}-playtest-settings.png']
        for filename in screenshots:
            if not re.fullmatch(r'(tv-onn|phone-samsung)-playtest-[a-z-]+\.png', filename):
                raise RuntimeError('Unexpected screenshot filename; refusing arbitrary file copy')
            adb('pull', f'{remote}/screenshots/{filename}', args.screenshots / filename)
        if report['failures'] == 0:
            print('Checking actual test-process restart and same-version package replacement', flush=True)
            initial = play_policy()
            if initial['settings_lock_enabled']:
                raise RuntimeError('Actual test installation unexpectedly enabled Settings Lock')
            prior_pid = isolated_pid()
            adb('shell', 'am', 'force-stop', PACKAGE)
            restarted = start_isolated()
            restarted_policy = play_policy()
            if restarted_policy != initial or (prior_pid is not None and restarted['pid'] == prior_pid):
                raise RuntimeError('Actual process restart did not preserve the tested policy')
            raw_stage_capture('post-restart')
            adb('shell', 'am', 'force-stop', PACKAGE)
            replacement_output = adb('install', '--user', '0', '-r', '-t', args.apk)
            replaced = start_isolated()
            replaced_policy = play_policy()
            if replaced_policy != initial or replaced['pid'] == restarted['pid']:
                raise RuntimeError('Same-version package replacement did not preserve tested preferences')
            raw_stage_capture('post-same-code-replacement')
            preservation = {'status': 'passed', 'before': initial, 'after_process_restart': restarted_policy,
                            'after_same_version_replacement': replaced_policy,
                            'previous_pid': prior_pid, 'restarted_pid': restarted['pid'],
                            'replacement_pid': replaced['pid'], 'package_replacement': replacement_output,
                            'scope': 'Real process restart and exact same APK reinstall preserve actual OFF flag and active playlist ID. This is not a versionCode-increasing upgrade test or an exhaustive database migration test.'}
            (evidence / 'restart-and-same-version-replacement.json').write_text(
                json.dumps(preservation, indent=2), encoding='utf-8')
        after = customer_state()
        unchanged = before == after
        (evidence / 'customer-installation-preserved.json').write_text(
            json.dumps({'before': before, 'after': after, 'unchanged': unchanged}, indent=2), encoding='utf-8')
        print(json.dumps({'device': identity, 'scope': report.get('scope', 'full'),
                          'tests': report['tests'], 'failures': report['failures'],
                          'customer_installation_unchanged': unchanged, 'evidence': str(evidence)}, indent=2), flush=True)
        if not unchanged:
            raise RuntimeError('Customer package installation metadata changed unexpectedly')
        if report['failures'] != 0:
            failed = [row for row in report['results'] if row['status'] == 'failed']
            print(json.dumps(failed, indent=2), flush=True)
            raise SystemExit(1)
    finally:
        # Leave the clearly named temporary test app installed, with no test playback running.
        adb('shell', 'am', 'force-stop', PACKAGE)
        if reverse_created:
            adb('reverse', '--remove', 'tcp:18765')


if __name__ == '__main__':
    main()
