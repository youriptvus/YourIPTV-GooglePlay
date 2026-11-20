> **Current November20:** Google Console confirms the closed test is published and the minimum12 opted-in tester requirement is complete. Android TV is Active. Publishing overview shows the submitted changes last published November19 and no pending changes. Production is still inactive: the14 continuous days requirement is incomplete and Apply for production is disabled. First observed12-person confirmation:November20,2:33PM Eastern; around December4 is an estimated eligibility check date, subject to continuous participation and Google's actual counter. No generated pre-launch report is available yet. Earlier November19 in-review/0-testers/recruitment-pause text below is historical. No GitHub, customer-source/device or protection changes were made in this check.

> **Current November19:** 1.0.9/code50 with the original full collage icon and automatic protection ON.17 changes, including the closed test and Android TV opt-in, are IN GOOGLE REVIEW. Production is NOT live;12 real closed testers/14 continuous days and production access remain required. See [current handoff](GooglePlay-Release-Ready/Reports/Publication-Handoff.md). Earlier records below are historical.

> Current final delivery (November 18): GooglePlay-Release-Ready/START-HERE.md and Reports/Final-Delivery-Report.md are authoritative. Signed artifacts, current phone/TV/public HTTPS/ATF checks and public privacy verification passed; owner Console/signing/declarations/testing/approval gates remain. Earlier phase numbers and pending-hosting statements below are historical.

# YourIPTV for Google Play

> **Final completion package:** [GooglePlay-Release-Ready/START-HERE.md](GooglePlay-Release-Ready/START-HERE.md) contains the current local release and manual Console sequence. Brand: YourIPTV; support/privacy: admin@youriptv.us; site: https://youriptv.us. Fresh completion results are recorded there; earlier code 45/48 results below remain historical.

> **Current status, November 17:** [Active Play 1.0.9 / proposed code 48](GooglePlay-v1.0.9/CURRENT_STATUS.md) is owner-approved local preparation, pending unused Console-history verification. Code 45 artifacts are historical. The latest final-completion request authorizes safe isolated checks; customer packages/data remain protected. The owner attributes the separate playback issue to the server; no encoder/decoder changes are included.

YourIPTV is an independent IPTV media player for Android phones, tablets, Android TV and Google TV. Add an Xtream Codes, M3U/M3U Plus playlist and XMLTV guide that you are authorized to use. No television channels, subscription or provider account is included. Normal player functionality is free, with no paywall, trial, playback timer or subscription requirement.

This independent project produces one application, `com.youriptv.app`, and one production AAB for all supported form factors. Touch and D-pad input use the shared player and security systems.

## Candidate status

| Item | Value |
|---|---|
| Baseline | Official v1.0.9, commit `0612c0acb500f096da5cfb766f6e7f0a012c4ba8`, code44 |
| Active preparation | 1.0.9 / proposed code 48; unused Console history and new release verification pending |
| Historical candidate | 1.0.9 / code 45; prior signed artifacts and recorded tests retained |
| Android | Minimum API23; compile/target API36 |
| Updates | Google Play; no app APK self-installer |
| Fresh Settings Lock | OFF; Admin management and parental gates independent |
| Repository | Local preparation only; the owner manually creates/verifies/pushes any PRIVATE youriptvus/YourIPTV-GooglePlay remote |

See [source provenance](SOURCE_BASELINE.md), [build/testing](docs/PLAY_BUILD_AND_TEST.md), [submission package](GooglePlay-v1.0.9/PLAY_STORE_README.md) and [test report](GooglePlay-v1.0.9/PLAY_STORE_TEST_REPORT.md). Imported customer documents are historical reference; Play documents supersede customer-only installation, provider shortcut, lock-default and updater instructions.

## Player features

Live TV, categories, previews and integrated guide; fullscreen playback and favorites; provider-supported catch-up, Movies and Series; playlist/EPG refresh and saved preferences; parental restrictions and provider limits; import/export and supported recording/reminders. Right on a TV remote recalls the prior channel in the current fullscreen live queue, and a second press toggles back. Existing recovery guards and retry timing remain.

Recording uses a provider connection. Reminders and scheduled recording retain the app-running limitation. Background refresh depends on Android scheduling/network/power conditions. Providers control content and guide coverage. See [known issues](docs/KNOWN_ISSUES.md); historical tests do not certify this candidate.

## Setup and security

Enter your own provider URL, username/password where needed, playlist name and guide URL. The secure Play candidate requires HTTPS for provider, playlist, guide, stream and artwork requests, and blocks insecure HTTP/downgrade redirects. Existing HTTP-only sources need a working HTTPS address from their provider. Keep playlists, credentials, private-account screenshots and backups private.

Fresh Play state opens normal Settings without a Settings PIN. Deliberate Settings Lock management remains Admin protected. Existing security state is preserved when freshness cannot be reliably established. Parental checks remain independent. See [security](docs/SETTINGS_SECURITY.md) for local Admin enrollment and legacy migration.

Credentials use app-private local storage; this is not database encryption. Exported JSON backups contain credentials. Automatic cloud backup is disabled, and explicit Android backup/device-transfer rules exclude the app's data. Manual export and compatible in-place updates retain their existing behavior. The offline in-app privacy notice explains data flows; owner must publish the matching privacy webpage before submission.

## Independent development

Build here without the customer source directory. Manually review future ports using [maintenance](docs/PLAY_MAINTENANCE.md); no automatic synchronization. Customer and Play apps share a package and normally cannot coexist; updates also require compatible signing and a higher code. Do not overwrite customer installs for QA.

Keys, passwords, SDK paths, test accounts and artifacts stay outside tracked source. Release signing reads external protected configuration via `YOURIPTV_UPLOAD_SIGNING_PROPERTIES`; no configuration means unsigned release output. Upload certificate and Play app-signing certificate have different roles. See [signing](GooglePlay-v1.0.9/PLAY_STORE_SIGNING.md).

The owner's latest instruction restricts this work to local operations. No GitHub creation, remote verification, push or upload is performed by workspace tools. The configured origin is retained for the owner's manual use; it does not prove a private repository exists. No application publication is performed.

## License

No source license has been selected by the owner. Free app functionality does not grant permission to redistribute source. Library licenses remain applicable. Use owner-approved artwork and authorized sample media for store assets.
