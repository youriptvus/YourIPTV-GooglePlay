"""Local authorized synthetic IPTV fixture. Excluded from production app assets.

Requires ffmpeg. Bind only loopback; use `adb -s SERIAL reverse tcp:18765 tcp:18765`.
No provider account, external media, customer data or network publishing is used.
"""
import argparse
from collections import Counter
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import subprocess
import tempfile
import threading
from urllib.parse import parse_qs, urlsplit

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ffmpeg', required=True)
    parser.add_argument('--port', type=int, default=18765)
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='youriptv-play-fixture-') as temp:
        video = Path(temp) / 'synthetic.ts'
        subprocess.run([args.ffmpeg, '-hide_banner', '-loglevel', 'error', '-f', 'lavfi', '-i',
            'testsrc2=size=640x360:rate=25', '-f', 'lavfi', '-i', 'sine=frequency=440:sample_rate=48000',
            '-t', '6', '-c:v', 'libx264', '-preset', 'ultrafast', '-threads', '2', '-pix_fmt', 'yuv420p',
            '-g', '25', '-c:a', 'aac', '-b:a', '64k', '-f', 'mpegts', str(video)], check=True)
        payload = video.read_bytes()
    counters = {'full_gets': Counter(), 'range_gets': Counter()}
    lock = threading.Lock()
    base = f'http://127.0.0.1:{args.port}'
    playlist = f'''#EXTM3U x-tvg-url="{base}/epg.xml"
#EXTINF:-1 tvg-id="synthetic1" group-title="Authorized test content",Synthetic Channel One
{base}/channel1.ts
#EXTINF:-1 tvg-id="synthetic2" group-title="Authorized test content",Synthetic Channel Two
{base}/channel2.ts
#EXTINF:-1 tvg-type="movie" group-title="Generated Movies",Synthetic Movie
{base}/movie/generated.ts
#EXTINF:-1 tvg-type="series" group-title="Generated Series",Synthetic Show S01E01
{base}/series/generated.ts
'''.encode()
    now = datetime.now(timezone.utc)
    start, stop = (now-timedelta(hours=1)).strftime('%Y%m%d%H%M%S +0000'), (now+timedelta(hours=8)).strftime('%Y%m%d%H%M%S +0000')
    epg = ('<?xml version="1.0" encoding="UTF-8"?><tv>' + ''.join(
        f'<channel id="synthetic{i}"><display-name>Synthetic Channel {i}</display-name></channel>'
        f'<programme start="{start}" stop="{stop}" channel="synthetic{i}"><title>Authorized generated test pattern</title><desc>Local test media generated for YourIPTV review. No television content.</desc></programme>'
        for i in (1,2)) + '</tv>').encode()
    class Handler(BaseHTTPRequestHandler):
        protocol_version = 'HTTP/1.1'
        def log_message(self, *_): pass
        def do_HEAD(self): self.respond(head=True)
        def do_GET(self): self.respond()
        def respond(self, head=False):
            request = urlsplit(self.path)
            path = request.path
            if path == '/stats':
                with lock: data = json.dumps(counters).encode()
                mime = 'application/json'
            elif path == '/player_api.php':
                # Entirely synthetic local API, never a provider account or production asset.
                action = parse_qs(request.query).get('action', [''])[0]
                if not action:
                    value = {'user_info': {'auth': 1, 'status': 'Active', 'max_connections': '1',
                             'active_cons': '0', 'exp_date': None}, 'server_info': {'timezone': 'UTC'}}
                elif action in ('get_live_categories', 'get_vod_categories', 'get_series_categories'):
                    value = [{'category_id': '1', 'category_name': 'Generated Xtream demo'}]
                elif action == 'get_live_streams':
                    value = [{'stream_id': '11', 'name': 'Synthetic Xtream Channel', 'category_id': '1',
                              'epg_channel_id': 'synthetic1', 'num': '1'}]
                elif action in ('get_vod_streams', 'get_series'):
                    value = []
                else:
                    self.send_error(404)
                    return
                data, mime = json.dumps(value).encode(), 'application/json'
            elif path == '/playlist.m3u': data, mime = playlist, 'audio/x-mpegurl'
            elif path == '/epg.xml': data, mime = epg, 'application/xml'
            elif path in ('/channel1.ts', '/channel2.ts', '/movie/generated.ts', '/series/generated.ts',
                          '/live/fixture/fixture/11.ts'): data, mime = payload, 'video/mp2t'
            else:
                self.send_error(404)
                return
            range_header = self.headers.get('Range')
            if not head:
                with lock: counters['range_gets' if range_header else 'full_gets'][path] += 1
            start_byte = 0
            if range_header and range_header.startswith('bytes='):
                start_byte = int(range_header[6:].split('-',1)[0] or '0')
            if start_byte >= len(data):
                self.send_error(416)
                return
            self.send_response(206 if range_header else 200)
            self.send_header('Content-Type', mime)
            self.send_header('Cache-Control', 'no-store')
            self.send_header('Connection', 'close')
            self.send_header('Accept-Ranges', 'bytes')
            if range_header: self.send_header('Content-Range', f'bytes {start_byte}-{len(data)-1}/{len(data)}')
            self.send_header('Content-Length', str(len(data)-start_byte))
            self.end_headers()
            if not head:
                try: self.wfile.write(data[start_byte:])
                except (ConnectionResetError, BrokenPipeError): pass
            self.close_connection = True
    server = ThreadingHTTPServer(('127.0.0.1', args.port), Handler)
    print(json.dumps({'fixtureBase': base, 'duration_seconds': 6, 'media_bytes':len(payload),
        'rights':'Synthetic testsrc2 video and sine audio generated locally; no external content'}), flush=True)
    try: server.serve_forever()
    except KeyboardInterrupt: pass
    finally: server.server_close()

if __name__ == '__main__': main()
