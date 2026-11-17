"""Prepare legal, offline reviewer files without publishing or bundling app content.

First run while run_review_fixture.py is active to copy locally generated media.
Later use --reuse-media to update the public-base playlist and regenerate a daily
XMLTV window without any network request. Outputs stay in ignored artifacts.
"""
import argparse
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import urlopen
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ARTIFACTS = ROOT / 'GooglePlay-v1.0.9/artifacts'
LOCAL_BASE = 'http://127.0.0.1:18765/'


def checked_base(value):
    if value == LOCAL_BASE:
        return value
    parsed = urlsplit(value)
    if (parsed.scheme != 'https' or not parsed.hostname or parsed.username or parsed.password
            or parsed.query or parsed.fragment):
        raise ValueError('Public base must be an owner-approved HTTPS directory URL without credentials, query or fragment')
    return value.rstrip('/') + '/'


def make_guide():
    now = datetime.now(timezone.utc).replace(minute=0, second=0, microsecond=0)
    first = now - timedelta(hours=1)
    last = now + timedelta(hours=48)
    root = ET.Element('tv', {'generator-info-name': 'YourIPTV authorized synthetic review fixture'})
    for number in (1, 2):
        channel_id = f'synthetic{number}'
        channel = ET.SubElement(root, 'channel', {'id': channel_id})
        ET.SubElement(channel, 'display-name').text = f'Synthetic Channel {number}'
        start = first
        while start < last:
            stop = start + timedelta(hours=1)
            programme = ET.SubElement(root, 'programme', {
                'start': start.strftime('%Y%m%d%H%M%S +0000'),
                'stop': stop.strftime('%Y%m%d%H%M%S +0000'), 'channel': channel_id,
            })
            ET.SubElement(programme, 'title').text = 'Authorized synthetic test pattern'
            ET.SubElement(programme, 'desc').text = 'Generated testsrc2 video and sine audio. No television channels or subscription content.'
            start = stop
    return ET.tostring(root, encoding='utf-8', xml_declaration=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ARTIFACTS / 'reviewer-demo')
    parser.add_argument('--public-base', help='Approved future HTTPS directory; setting this does not upload any file')
    parser.add_argument('--reuse-media', action='store_true', help='Use existing synthetic TS files; regenerate only playlist, guide and notes')
    args = parser.parse_args()
    destination = args.output.resolve()
    if not destination.is_relative_to(ARTIFACTS.resolve()):
        raise ValueError('Reviewer output must stay inside this project\'s ignored artifacts directory')
    base = checked_base(args.public_base or LOCAL_BASE)
    destination.mkdir(parents=True, exist_ok=True)
    if args.reuse_media:
        for name in ('channel1.ts', 'channel2.ts'):
            if not (destination / name).is_file():
                raise ValueError('Synthetic media is missing; first export while the local fixture is active')
    else:
        for name in ('channel1.ts', 'channel2.ts'):
            with urlopen(LOCAL_BASE + name, timeout=30) as response:
                payload = response.read()
            if not payload or len(payload) % 188 != 0 or payload[0] != 0x47:
                raise ValueError('Local fixture did not return a valid MPEG-TS payload')
            (destination / name).write_bytes(payload)
    playlist = f'''#EXTM3U x-tvg-url="{base}epg.xml"
#EXTINF:-1 tvg-id="synthetic1" group-title="Authorized review content",Synthetic Channel One
{base}channel1.ts
#EXTINF:-1 tvg-id="synthetic2" group-title="Authorized review content",Synthetic Channel Two
{base}channel2.ts
'''
    (destination / 'playlist.m3u').write_text(playlist, encoding='utf-8')
    (destination / 'epg.xml').write_bytes(make_guide())
    local_only = base == LOCAL_BASE
    readme = f'''# Authorized synthetic YourIPTV reviewer demo

Status: {'LOCAL DEVELOPMENT ONLY - loopback is unreachable to remote reviewers.' if local_only else 'OFFLINE HOSTING PREPARATION ONLY - no upload or public reachability has been verified.'}

Configured base: {base}

The two MPEG-TS files contain the same six-second synthetic testsrc2 pattern and sine audio generated locally with FFmpeg. No television programme, commercial channel, customer provider, account, logo or third-party movie is included. Media is outside application assets and is not bundled into the production AAB. The owner may use these generated demo files for authorized review.

Files to host together: playlist.m3u, epg.xml, channel1.ts and channel2.ts. The guide uses matching synthetic1/synthetic2 IDs and a rolling UTC window; regenerate daily while review access is active. The app reconnects when each finite TS sample ends. This fixture is useful for guide/live/toggle behavior, not a proof of Xtream, catch-up, Movies or Series access.

Before public use, select an owner-controlled stable HTTPS directory and run from the source root:

    py scripts/export_review_demo.py --reuse-media --public-base https://YOUR-APPROVED-HOST.example/youriptv-review/

Replace the example hostname before any upload. Uploading or deploying requires separate owner authorization. No deployment is performed by this script. Preserve HTTPS, correct MIME types, byte-range support where available, and access without a login, expiring link, geofence or home-LAN dependency. Verify all four public files from outside the home network and test the exact playlist on phone and TV before entering App access instructions.

Repeat the --reuse-media command daily, then deploy only the updated epg.xml through the approved hosting process. Confirm timestamps and reachability throughout Google's review. Never add customer credentials or subscription content to this folder.
'''
    (destination / 'README.md').write_text(readme, encoding='utf-8')
    manifest = {
        'prepared_at_utc': datetime.now(timezone.utc).isoformat(),
        'base_url': base, 'local_development_only': local_only, 'deployed': False,
        'rights': 'Locally generated synthetic testsrc2 video and sine audio; no external media',
        'files': {name: {'bytes': (destination / name).stat().st_size,
                         'sha256': hashlib.sha256((destination / name).read_bytes()).hexdigest()}
                  for name in ('playlist.m3u', 'epg.xml', 'channel1.ts', 'channel2.ts', 'README.md')},
    }
    (destination / 'demo-manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    print(json.dumps({'output': str(destination), 'base': base, 'local_development_only': local_only,
                      'deployed': False, 'media_bytes': manifest['files']['channel1.ts']['bytes']}, indent=2))


if __name__ == '__main__':
    main()
