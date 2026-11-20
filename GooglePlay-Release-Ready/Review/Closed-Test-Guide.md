> **Current November20:** Google Console confirms the closed test is published and the minimum12 opted-in tester requirement is complete. Android TV is Active. Publishing overview shows the submitted changes last published November19 and no pending changes. Production is still inactive: the14 continuous days requirement is incomplete and Apply for production is disabled. First observed12-person confirmation:November20,2:33PM Eastern; around December4 is an estimated eligibility check date, subject to continuous participation and Google's actual counter. No generated pre-launch report is available yet. Earlier November19 in-review/0-testers/recruitment-pause text below is historical. No GitHub, customer-source/device or protection changes were made in this check.

# Closed test guide — YourIPTV 1.0.9/code50

November20: **the closed release is live for the selected testers and Google confirms the minimum12 opted-in requirement is complete.** The next phase is real installation, feature testing and feedback during14 continuous days. The release page identifies1.0.9/code50 as available to selected testers, released November19 at5:11PM Eastern. Production remains inactive. This is a test plan, not evidence that the participants have installed or passed any scenario.

## Install and test now

1. Use the Google account added to the closed-test list on both the opt-in page and the Android device's Play Store. The selected closed release is available now.
2. If enrolled in internal testing, opt out of that program first. Internal participants cannot receive the closed test while still enrolled there. Do not uninstall or clear an existing customer installation just to switch tracks.
3. Open [the closed-test opt-in page](https://play.google.com/apps/testing/com.youriptv.app), join, and use its Google Play installation link. This differs from the internal-test link.
4. Confirm the app identifies as YourIPTV1.0.9/code50 and uses the original collage icon. If Play reports a signing/update problem, report it; do not remove an existing installation/data to force the install.
5. Remain opted in continuously for at least14 days and actively exercise the app. Google Console is authoritative for the eligibility count; email-list membership and installation alone do not establish continuous opt-in. At least12 real participants are required before applying for production access.

First12-person confirmation was observed November20 at2:33PM Eastern. Around December4 is a planning estimate for checking eligibility, not a guaranteed unlock/publication date. Do not leave/rejoin the closed program during the continuous period. Report installation/update errors instead of clearing existing app data.

## First test session

1. Install or update through the Google Play link on the closed-test page, then launch YourIPTV. Confirm the original collage icon and About version1.0.9/code50.
2. Add an authorized HTTPS M3U playlist or Xtream source. A tester without a provider can use the public synthetic playlist and guide below. No paid YourIPTV account or activation is required.
3. Open the guide and a live preview, enter fullscreen, switch channels, then use Back. On TV, check D-pad navigation and Right previous-channel. Try a movie and a series episode where the source offers them.
4. Background/return to the app and relaunch it; check that source settings remain. Confirm normal fresh Settings opens without an Admin PIN. Only test optional PIN restrictions on a configuration you control.
5. Submit actual results through private Google Play feedback or admin@youriptv.us, including device/Android version, feature and reproducible steps. Keep using the app across the test period; report bugs as they happen.

## Safe functional checks

Use your own authorized HTTPS source. The optional [public synthetic playlist](https://youriptv.us/review-media/playlist.m3u) and [guide](https://youriptv.us/review-media/epg.php) contain generated test patterns, not commercial channels or subscriptions. A November20 HTTPS check returned200 for both,4 playlist items and2 guide channels/384 programmes, without redirects or HTTP playlist entries. This checks source availability, not actual Play-delivered playback. They can exercise live/movie/series UI but do not prove authenticated Xtream, provider catch-up or every codec works.

| Area | What to check |
|---|---|
| Phone and TV startup | Play installation, original icon, touch/D-pad navigation, readable layout and no crash |
| Source setup | M3U/Xtream with an authorized HTTPS provider, useful validation errors, catalogue and guide refresh |
| Playback | Live preview/fullscreen, movie/series navigation, channel switching/Right previous-channel on TV, pause/resume/Back and interruptions |
| Supported source features | Catch-up and recording only where the provider/device supports them; report unsupported cases accurately |
| Settings and security | Fresh Settings Lock OFF; enable optional Admin/parental controls deliberately, verify them without changing anyone else's PIN or sources |
| Lifecycle | Background/foreground, rotation where applicable, relaunch, saved preferences and safe connection recovery |
| Backup | Use a test configuration; explicit exports contain readable source credentials and must stay private. PINs are excluded. Never send backup files or source passwords as feedback |
| Play delivery | Google-protected installed app starts and works without sideloading, disabling protection or bypassing checks |

Send actual findings to **admin@youriptv.us** or private Google Play test feedback. Include device/model, Android version, app version/code, feature, reproduction steps, expected/observed behavior and whether reproducible. Redact credentials and private playlist URLs from screenshots/logs.

Keep an owner-local feedback log using `Closed-Test-Feedback-Template.csv`. Leave unperformed checks blank/untested; do not prefill passes or fabricate engagement. Address real defects, review pre-launch findings, then use actual results for the later production-access application.

Owner Console feedback page: `Monitor and improve > Ratings and reviews > Testing feedback` (app route `user-feedback/beta-feedback`). No participant findings have been recorded by workspace tools. Console pre-launch settings retain no developer-login credentials and no untested custom script; the app has no YourIPTV account gate. A generated pre-launch report is still unavailable. These facts do not certify stability or replace participant feedback.

Official guidance: [test setup and internal/closed eligibility](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en), [12 testers/14 continuous days](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en).
