"""Refresh local, synthetic reviewer metadata. This script never uploads files."""
import argparse
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
from urllib.parse import urlsplit
import xml.etree.ElementTree as ET

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--public-base', default='https://youriptv.us/review-media/')
    args = parser.parse_args()
    parsed = urlsplit(args.public_base)
    if parsed.scheme != 'https' or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment:
        parser.error('Use an HTTPS directory without credentials, query or fragment')
    base = args.public_base.rstrip('/') + '/'
    folder = Path(__file__).resolve().parent
    for name in ('channel1.ts', 'channel2.ts'):
        content = (folder/name).read_bytes()
        if not content or len(content) % 188 or content[0] != 0x47:
            raise RuntimeError('Expected generated MPEG-TS media is missing or invalid')
    playlist = f'''#EXTM3U x-tvg-url="{base}epg.xml"
#EXTINF:-1 tvg-id="synthetic1" group-title="Authorized review content",Synthetic Channel One
{base}channel1.ts
#EXTINF:-1 tvg-id="synthetic2" group-title="Authorized review content",Synthetic Channel Two
{base}channel2.ts
#EXTINF:-1 tvg-type="movie" group-title="Authorized synthetic movies",YourIPTV Synthetic Movie (six-second demo)
{base}channel1.ts?review-kind=movie
#EXTINF:-1 tvg-type="series" group-title="Authorized synthetic series",YourIPTV Synthetic Series S01E01
{base}channel2.ts?review-kind=episode
'''
    (folder/'playlist.m3u').write_text(playlist, encoding='utf-8')
    now = datetime.now(timezone.utc).replace(minute=0, second=0, microsecond=0)
    tv = ET.Element('tv', {'generator-info-name': 'YourIPTV locally generated authorized demonstration'})
    for number in (1, 2):
        channel_id = f'synthetic{number}'
        channel = ET.SubElement(tv, 'channel', {'id': channel_id})
        ET.SubElement(channel, 'display-name').text = f'Synthetic Channel {number}'
        start = now - timedelta(hours=1)
        for _ in range(49):
            stop = start + timedelta(hours=1)
            programme = ET.SubElement(tv, 'programme', {'start': start.strftime('%Y%m%d%H%M%S +0000'), 'stop': stop.strftime('%Y%m%d%H%M%S +0000'), 'channel': channel_id})
            ET.SubElement(programme, 'title').text = 'Authorized synthetic test pattern'
            ET.SubElement(programme, 'desc').text = 'Generated test pattern and sine audio; no television, film or subscription content.'
            start = stop
    (folder/'epg.xml').write_bytes(ET.tostring(tv, encoding='utf-8', xml_declaration=True))
    excluded = {'checksums.json', 'completion-checksums.json', 'SHA256SUMS.txt'}
    files = {p.name: {'bytes': p.stat().st_size, 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(folder.iterdir()) if p.is_file() and p.name not in excluded}
    report = {'generatedUtc': datetime.now(timezone.utc).isoformat(), 'publicBase': base, 'deployed': False, 'reachabilityVerified': False, 'files': files}
    for name in ('checksums.json', 'completion-checksums.json'):
        (folder/name).write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    (folder/'SHA256SUMS.txt').write_text(''.join(row['sha256']+'  '+name+'\n' for name,row in files.items()), encoding='ascii')
    print(json.dumps({'localFilesRefreshed': len(files), 'publicBase': base, 'uploaded': False}))

if __name__ == '__main__':
    main()
