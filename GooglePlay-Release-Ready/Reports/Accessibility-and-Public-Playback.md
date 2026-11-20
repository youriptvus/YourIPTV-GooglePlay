# Actual public HTTPS and accessibility tests

| Device | Checks | Failures | Google ATF errors | Warnings | Information |
|---|---:|---:|---:|---:|---:|
| R5GL75PGMFP | 14 | 0 | 0 | 1 | 8 |
| 192.168.1.150:5555 | 14 | 0 | 0 | 1 | 8 |

Google Accessibility Test Framework 4.1.1 LATEST checks ran on main, Settings, About, Privacy and live/movie/episode player screens. Real Tab/Shift-Tab focus checks also passed. All public playlist/guide imports and decoded-frame playback checks passed, with actual EOF/activity completion for movie and episode. Individual findings, including advisory text-expansion warnings, are retained in Public-Playback-ATF-*.json. ATF is an androidTest-only dependency and is absent from the signed production DEX.

Corrections: 48dp navigation/group/guide targets and adequate portrait group space; hidden empty action scroller; readable 14sp Media3 seek labels; a single VOD accessibility container while preserving descendant controls and live root actions. All original failed audits are retained. This automated scope does not certify manual TalkBack, switch access, all screens/font scales or Google approval.

Public source: https://youriptv.us/review-media/playlist.m3u. Rolling guide: https://youriptv.us/review-media/epg.php. Both are credential-free generated review examples, not provider subscriptions. Policy paragraph verification and response hashes are in Public-ReadOnly-Verification.json. No deployment occurred during this continuation.
