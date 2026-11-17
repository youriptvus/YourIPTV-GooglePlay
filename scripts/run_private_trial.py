"""Private, sequential .playtest validation; never log provider inputs or raw responses.

The input is a CurrentUser DPAPI-encrypted playlist URL outside this repository.
--probe-only performs account metadata GETs only; it never opens a stream.
"""
import argparse
import datetime
import gzip
import json
import os
import re
from pathlib import Path
import secrets
import ssl
import subprocess
import sys
import time
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'com.youriptv.app.playtest'
TEST_PACKAGE = PACKAGE + '.test'


class RejectSecureDowngrade(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, new_url):
        if urllib.parse.urlsplit(request.full_url).scheme == 'https' and urllib.parse.urlsplit(new_url).scheme != 'https':
            raise RuntimeError('Secure metadata transport was unavailable')
        return super().redirect_request(request, fp, code, message, headers, new_url)


def decrypt_url(path):
    # Only the protected file path is passed to a subprocess, never the credential URL.
    command = r'''$ErrorActionPreference='Stop'; [Console]::OutputEncoding=[Text.UTF8Encoding]::new($false);
    $cipher=(Get-Content -LiteralPath $env:YOURIPTV_PRIVATE_TRIAL_INPUT -Raw).Trim();
    $secure=ConvertTo-SecureString -String $cipher;
    $pointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure);
    try {[Console]::Out.Write([Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer))}
    finally {[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)}'''
    env = dict(os.environ, YOURIPTV_PRIVATE_TRIAL_INPUT=str(path.resolve()))
    result = subprocess.run(['powershell.exe', '-NoLogo', '-NoProfile', '-NonInteractive', '-Command', command],
                            capture_output=True, env=env, timeout=30)
    if result.returncode:
        raise RuntimeError('Protected trial input could not be decoded')
    value = result.stdout.decode('utf-8-sig').strip()
    parts = urllib.parse.urlsplit(value)
    query = urllib.parse.parse_qs(parts.query)
    if parts.scheme not in {'http', 'https'} or not parts.hostname or parts.username or not query.get('username') or not query.get('password'):
        raise RuntimeError('Protected trial input is not a supported authenticated playlist')
    return value


def endpoint(playlist, filename):
    parts = urllib.parse.urlsplit(playlist)
    query = urllib.parse.parse_qs(parts.query)
    directory = parts.path.rsplit('/', 1)[0] + '/'
    return urllib.parse.urlunsplit((parts.scheme, parts.netloc, directory + filename,
        urllib.parse.urlencode({'username': query['username'][0], 'password': query['password'][0]}), ''))


def nonnegative(value):
    try:
        return max(0, int(value))
    except (TypeError, ValueError, OverflowError):
        return None


def account_metadata(playlist):
    request = urllib.request.Request(endpoint(playlist, 'player_api.php'),
                                     headers={'User-Agent': 'YourIPTV/1.0.9 (private validation)'})
    opener = urllib.request.build_opener(RejectSecureDowngrade(), urllib.request.HTTPSHandler(context=ssl.create_default_context()))
    with opener.open(request, timeout=15) as response:
        body = response.read(1024 * 1024 + 1)
        transport = urllib.parse.urlsplit(response.geturl()).scheme
    if len(body) > 1024 * 1024:
        raise RuntimeError('Account metadata exceeded the safe response limit')
    parsed = json.loads(body)
    info = parsed.get('user_info', {}) if isinstance(parsed, dict) else {}
    auth = str(info.get('auth', '')).lower() in {'1', 'true'}
    status = str(info.get('status', '')).casefold()
    status = status if status in {'active', 'banned', 'disabled', 'expired'} else 'unknown'
    expires = nonnegative(info.get('exp_date'))
    return {'authenticated': auth, 'status': status, 'maximumConnections': nonnegative(info.get('max_connections')),
            'activeConnections': nonnegative(info.get('active_cons')), 'metadataTransport': transport,
            'expiryInFuture': expires > time.time() if expires else None,
            'allowedOutputFormats': [value for value in info.get('allowed_output_formats', []) if value in {'ts', 'm3u8', 'rtmp'}]}


def choose_transport(playlist):
    source = urllib.parse.urlsplit(playlist)
    if source.scheme == 'http':
        secure_netloc = source.netloc
        if source.port == 80:
            secure_netloc = source.hostname
        candidate = urllib.parse.urlunsplit(source._replace(scheme='https', netloc=secure_netloc))
        try:
            metadata = account_metadata(candidate)
            if metadata['authenticated'] and metadata['status'] == 'active':
                return candidate, metadata
        except Exception:
            pass  # No URL, exception text, or raw response leaves memory.
    return playlist, account_metadata(playlist)


def output_kind(playlist):
    value = urllib.parse.parse_qs(urllib.parse.urlsplit(playlist).query).get('output', [''])[0].casefold()
    return value if value in {'ts', 'mpegts', 'mpeg-ts', 'm3u8'} else 'other'


def with_ts_output(playlist, metadata):
    if 'ts' not in metadata.get('allowedOutputFormats', []) or output_kind(playlist) not in {'ts', 'mpegts', 'mpeg-ts'}:
        raise RuntimeError('Canonical MPEG-TS output is not authorized by the account metadata')
    parts = urllib.parse.urlsplit(playlist)
    parameters = [(key, 'ts' if key == 'output' else value) for key, value in urllib.parse.parse_qsl(parts.query, keep_blank_values=True)]
    return urllib.parse.urlunsplit(parts._replace(query=urllib.parse.urlencode(parameters)))


def playlist_metadata(playlist):
    """Bounded metadata GET only. Never follow a redirect to a media stream."""
    class MetadataRedirect(RejectSecureDowngrade):
        redirects = 0
        def redirect_request(self, request, fp, code, message, headers, new_url):
            self.redirects += 1
            path = urllib.parse.unquote(urllib.parse.urlsplit(new_url).path).casefold()
            if '/live/' in path or path.endswith(('.ts', '.mp4', '.mkv', '.avi', '.mpeg')):
                raise RuntimeError('Media redirect refused for metadata-only probe')
            return super().redirect_request(request, fp, code, message, headers, new_url)
    redirects = MetadataRedirect()
    opener = urllib.request.build_opener(redirects, urllib.request.HTTPSHandler(context=ssl.create_default_context()))
    started = time.monotonic()
    result = {'requestedOutputKind': output_kind(playlist), 'hostMediaStreamOpened': False}
    try:
        request = urllib.request.Request(playlist, headers={'User-Agent': 'YourIPTV/0.1 (Android)', 'Accept-Encoding': 'gzip'})
        with opener.open(request, timeout=20) as response:
            kind = response.headers.get_content_type().casefold()
            result.update({'httpStatus': response.status, 'transport': urllib.parse.urlsplit(response.geturl()).scheme,
                           'contentKind': 'video' if kind.startswith('video/') else kind if kind in {'text/plain', 'text/html', 'application/json', 'application/octet-stream', 'audio/x-mpegurl', 'application/x-mpegurl', 'application/vnd.apple.mpegurl'} else 'other',
                           'declaredLength': nonnegative(response.headers.get('Content-Length')),
                           'gzipDeclared': response.headers.get('Content-Encoding', '').casefold() == 'gzip'})
            if kind.startswith('video/'):
                result.update({'sampleReadBytes': 0, 'isExtendedM3u': False, 'status': 'media_content_refused'})
            else:
                source = gzip.GzipFile(fileobj=response) if result['gzipDeclared'] else response
                body = source.read(256 * 1024)
                trimmed = body.lstrip(b'\xef\xbb\xbf \t\r\n')
                result.update({'sampleReadBytes': len(body), 'sampleLimitBytes': 256 * 1024,
                               'isExtendedM3u': trimmed.startswith(b'#EXTM3U'),
                               'sampledEntries': body.count(b'#EXTINF:'),
                               'bodyKind': 'm3u' if trimmed.startswith(b'#EXTM3U') else 'html' if trimmed[:32].lower().startswith((b'<html', b'<!doctype html')) else 'json' if trimmed.startswith((b'{', b'[')) else 'other',
                               'status': 'metadata_read'})
    except Exception as error:
        result.update({'status': 'failed', 'errorType': type(error).__name__})
        if hasattr(error, 'code') and isinstance(error.code, int):
            result['httpStatus'] = error.code
    result.update({'redirectCount': redirects.redirects, 'elapsedMs': int((time.monotonic() - started) * 1000)})
    return result


def assert_sanitized(value, playlist):
    parameters = urllib.parse.parse_qs(urllib.parse.urlsplit(playlist).query)
    private_values = {item for key in ('username', 'password') for item in parameters.get(key, []) if len(item) >= 4}
    private_values |= {urllib.parse.quote(item, safe='') for item in private_values}
    forbidden_keys = {'username', 'password', 'playlisturl', 'epgurl', 'channelname', 'title', 'rawresponse', 'errormessage', 'stacktrace'}
    def visit(item):
        if isinstance(item, dict):
            if any(str(key).casefold() in forbidden_keys for key in item):
                raise RuntimeError('Private evidence contained an unsupported field')
            for child in item.values():
                visit(child)
        elif isinstance(item, list):
            for child in item:
                visit(child)
        elif isinstance(item, str):
            if '://' in item or any(secret in item for secret in private_values):
                raise RuntimeError('Private evidence failed the credential exclusion check')
        elif item is not None and not isinstance(item, (int, float, bool)):
            raise RuntimeError('Private evidence contained an unsupported value')
    visit(value)


def bounded_live_subset(playlist):
    """Hold a few authorized live entries in memory; never open their media URLs."""
    request = urllib.request.Request(playlist, headers={'User-Agent': 'YourIPTV/0.1 (Android)', 'Accept-Encoding': 'gzip'})
    opener = urllib.request.build_opener(RejectSecureDowngrade(), urllib.request.HTTPSHandler(context=ssl.create_default_context()))
    with opener.open(request, timeout=20) as response:
        if response.headers.get_content_type().startswith('video/'):
            raise RuntimeError('A playlist metadata response was required')
        source = gzip.GzipFile(fileobj=response) if response.headers.get('Content-Encoding', '').casefold() == 'gzip' else response
        body = source.read(256 * 1024).decode('utf-8-sig', errors='replace')
    if not body.lstrip().startswith('#EXTM3U'):
        raise RuntimeError('The bounded private metadata was not an extended playlist')
    adult = re.compile(r'(?<!\w)(adults?|xxx+|porn\w*|erotic\w*|playboy|hustler|nsfw|18\s*\+|\+\s*18|for\s+adults|adults?\s+only)(?!\w)', re.I)
    pending = None
    selected = []
    for line in body.splitlines():
        line = line.strip()
        if line.startswith('#EXTINF:'):
            pending = line
        elif pending and line.startswith(('http://', 'https://')):
            attributes = dict(re.findall(r'([A-Za-z0-9_-]+)="([^"]*)"', pending))
            group = attributes.get('group-title', '')
            title = pending.rsplit(',', 1)[-1]
            safe_text = re.sub(r'adult\s*swim|young\s+adults?', ' ', group + ' ' + title, flags=re.I)
            path = urllib.parse.urlsplit(line).path.casefold()
            kind = attributes.get('tvg-type', '').casefold()
            if not adult.search(safe_text) and kind not in {'movie', 'movies', 'series'} and '/movie/' not in path and '/series/' not in path and not path.endswith(('.mp4', '.mkv', '.avi', '.m3u8')):
                selected.append(f'#EXTINF:-1 tvg-type="live" group-title="{group}",Private validation {len(selected) + 1}\n{line}\n')
                if len(selected) == 3:
                    break
            pending = None
    if not selected:
        raise RuntimeError('No safe bounded private live entries were available')
    return '#EXTM3U\n' + ''.join(selected), len(selected)


def execute_device(args, playlist, metadata):
    def adb(*command, timeout=120, data=None):
        result = subprocess.run([str(args.adb), '-s', args.serial, *map(str, command)],
                                input=data, capture_output=True, timeout=timeout)
        if result.returncode:
            raise RuntimeError('Selected isolated device operation failed')
        return result.stdout.decode('utf-8', errors='replace').strip()

    if adb('get-state') != 'device':
        raise RuntimeError('The explicitly selected device is unavailable')
    manufacturer = adb('shell', 'getprop', 'ro.product.manufacturer')
    if manufacturer.casefold() != args.expected_manufacturer.casefold():
        raise RuntimeError('The selected device manufacturer did not match')
    instrumentation = adb('shell', 'pm', 'list', 'instrumentation')
    component = f'{TEST_PACKAGE}/com.youriptv.app.TrialPlaybackInstrumentation'
    if component not in instrumentation:
        raise RuntimeError('The separate private trial instrumentation is not installed')
    def customer_state():
        text = adb('shell', 'dumpsys', 'package', 'com.youriptv.app')
        return [line.strip() for line in text.splitlines()
                if any(key in line for key in ('versionCode=', 'versionName=', 'firstInstallTime=', 'lastUpdateTime='))]
    before = customer_state()
    identity = {'serial': args.serial, 'manufacturer': manufacturer,
                'model': adb('shell', 'getprop', 'ro.product.model'), 'sdk': adb('shell', 'getprop', 'ro.build.version.sdk')}
    nonce = secrets.token_hex(16)
    private_input = {'nonce': nonce, 'playlistUrl': playlist, 'epgUrl': endpoint(playlist, 'xmltv.php'),
                     'account': metadata, 'observeSeconds': args.observe_seconds}
    if args.bounded_live_subset:
        private_input['subsetM3u'], private_input['subsetEntryCount'] = bounded_live_subset(playlist)
    payload = json.dumps(private_input).encode('utf-8')
    report = None
    protocol_finished = False
    try:
        adb('shell', 'am', 'force-stop', PACKAGE)
        # Remote shell command is fixed; private values travel only over standard input.
        adb('shell', f"run-as {PACKAGE} sh -c 'umask 077; mkdir -p files; cat > files/trial-input.json'", data=payload)
        adb('shell', 'am', 'instrument', '-w', '-e', 'runId', nonce, component,
            timeout=900 + args.observe_seconds)
        raw = adb('shell', 'run-as', PACKAGE, 'cat', 'files/trial-result.json')
        report = json.loads(raw)
        if report.get('runId') != nonce:
            raise RuntimeError('No fresh private trial report was produced')
        assert_sanitized(report, playlist)
        protocol_finished = True
    finally:
        adb('shell', 'am', 'force-stop', PACKAGE)
        # Remove only this helper's two fixed private-input/report files.
        adb('shell', 'run-as', PACKAGE, 'rm', '-f', 'files/trial-input.json', 'files/trial-result.json')
        after = customer_state()
        preserved = {'unchanged': before == after, 'before': before, 'after': after}
        (args.output / 'customer-installation-preserved.json').write_text(json.dumps(preserved, indent=2), encoding='utf-8')
    if not protocol_finished or report is None:
        raise RuntimeError('Private trial did not finish with a sanitized report')
    released = False
    after_account = None
    for _ in range(9):
        try:
            after_account = account_metadata(playlist)
            if after_account['activeConnections'] == 0:
                released = True
                break
        except Exception:
            pass
        time.sleep(5)
    evidence = {'device': identity, 'accountBefore': metadata, 'accountAfter': after_account,
                'providerConnectionReleased': released, 'trial': report,
                'customerInstallationUnchanged': preserved['unchanged'],
                'scope': 'One private stream at a time in an isolated test package. No screenshots, raw network bodies, raw process output, or logcat captured.'}
    assert_sanitized(evidence, playlist)
    (args.output / 'private-trial-results.json').write_text(json.dumps(evidence, indent=2), encoding='utf-8')
    passed = report.get('failures') == 0 and preserved['unchanged'] and released
    print(json.dumps({'status': 'passed' if passed else 'failed', 'device': identity,
                      'checks': len(report.get('checks', [])), 'failures': report.get('failures'),
                      'providerConnectionReleased': released, 'customerInstallationUnchanged': preserved['unchanged'],
                      'evidence': str(args.output / 'private-trial-results.json')}, indent=2))
    if not passed:
        raise SystemExit(1)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--private-config', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--probe-only', action='store_true')
    parser.add_argument('--probe-playlist', action='store_true')
    parser.add_argument('--canonical-output-ts', action='store_true')
    parser.add_argument('--bounded-live-subset', action='store_true')
    parser.add_argument('--serial')
    parser.add_argument('--expected-manufacturer')
    parser.add_argument('--adb', type=Path)
    parser.add_argument('--observe-seconds', type=int, default=60)
    args = parser.parse_args()
    if args.private_config.resolve().is_relative_to(ROOT) or args.output.resolve().is_relative_to(ROOT):
        raise RuntimeError('Private input and trial evidence must remain outside the repository')
    if not 20 <= args.observe_seconds <= 180:
        raise RuntimeError('Trial observation must be bounded between 20 and 180 seconds')
    playlist, metadata = choose_transport(decrypt_url(args.private_config))
    args.output.mkdir(parents=True, exist_ok=True)
    probe = {'generatedUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'scope': 'Account metadata only; no host media stream opened',
             'account': metadata, 'eligibleForOneIsolatedStream': metadata['authenticated'] and metadata['status'] == 'active' and metadata['activeConnections'] == 0}
    (args.output / 'account-preflight.json').write_text(json.dumps(probe, indent=2), encoding='utf-8')
    if args.probe_playlist:
        probes = {'account': metadata, 'original': playlist_metadata(playlist)}
        if output_kind(playlist) in {'mpegts', 'mpeg-ts'} and 'ts' in metadata.get('allowedOutputFormats', []):
            probes['canonicalTs'] = playlist_metadata(with_ts_output(playlist, metadata))
        assert_sanitized(probes, playlist)
        (args.output / 'playlist-metadata-probe.json').write_text(json.dumps(probes, indent=2), encoding='utf-8')
        print(json.dumps(probes, indent=2))
        return
    if args.probe_only:
        print(json.dumps(probe, indent=2))
        return
    if not probe['eligibleForOneIsolatedStream']:
        raise RuntimeError('Trial account is not currently eligible for a single isolated stream')
    if not args.serial or not args.expected_manufacturer or not args.adb:
        raise RuntimeError('An explicitly selected device, manufacturer, and ADB binary are required')
    if args.canonical_output_ts:
        playlist = with_ts_output(playlist, metadata)
    execute_device(args, playlist, metadata)


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        # Exception messages and tracebacks may contain URLs. Emit the type only.
        print(json.dumps({'status': 'failed', 'errorType': type(error).__name__}))
        sys.exit(1)
