# Reviewer access and legal demonstration

App starts without bundled content/subscription. Fresh normal Settings requires no Settings PIN. Reviewer access must demonstrate real features using legal, persistent sources.

Use a stable owner-controlled HTTPS directory hosting a small M3U/M3U Plus playlist and XMLTV guide with matching tvg-id. Supply at least two clearly named legal sample items. No customer credentials or preset subscription server.

## Prepared synthetic demonstration

The ignored `artifacts/reviewer-demo/` folder contains two six-second synthetic MPEG-TS samples, an M3U playlist, matching XMLTV guide, checksums and hosting instructions. The video test pattern and sine audio were generated locally; there is no external television/movie media, subscription server or customer account. These files are outside application assets and are not bundled into the AAB.

The initial export uses `http://127.0.0.1:18765/` and is **LOCAL DEVELOPMENT ONLY**. Loopback is not reachable to remote reviewers. The source script prepares a future approved HTTPS base and regenerates the guide without publishing anything:

```powershell
py scripts/export_review_demo.py --reuse-media --public-base https://YOUR-APPROVED-HOST.example/youriptv-review/
```

Replace the example hostname with an owner-controlled HTTPS directory before an authorized deployment. Host `playlist.m3u`, `epg.xml`, `channel1.ts` and `channel2.ts` together. Regenerate/deploy the guide daily during review and verify all files from outside the home network. Follow the exported README for exact commands, MIME/range behavior and rights record. No deployment or public-reachability pass is claimed.

The synthetic fixture can demonstrate guide, live playback, reconnects and previous-channel switching. It does not demonstrate a real Xtream account, provider catch-up, Movies or Series; supply stable authorized full-feature access where review requires it. Local success is not proof of deployed reviewer access.

## Console App access draft

1. Open YourIPTV. No YourIPTV registration, purchase or subscription.
2. Open Settings and playlist management; choose generic M3U setup.
3. Name `YourIPTV Review`; use `[owner stable HTTPS M3U]` and `[matching XMLTV]`, if not advertised by M3U. Save/refresh.
4. Open guide, select a demo item and fullscreen playback. Touch on mobile; D-pad/OK on TV. After watching two live items in the same queue, Right recalls previous channel.
5. Open normal Settings without PIN on fresh state. Admin/parental functions remain separate; do not enable gates without corresponding review instructions.

Replace placeholders and verify exact final labels. Any full-feature reviewer account must be non-personal, authorized, stable/reusable, without expiring OTP/geofence/home-LAN dependency. Keep credentials outside Git. No working public demo deployment/reviewer-account pass claimed.
