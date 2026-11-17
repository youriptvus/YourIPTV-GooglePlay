# YourIPTV for Google Play

YourIPTV is an independent IPTV media player for Android phones, tablets, Android TV and Google TV. Add an Xtream Codes, M3U/M3U Plus playlist and XMLTV guide that you are authorized to use. No television channels, subscription or provider account is included. Normal player functionality is free, with no paywall, trial, playback timer or subscription requirement.

This independent project produces one application, `com.youriptv.app`, and one production AAB for all supported form factors. Touch and D-pad input use the shared player and security systems.

## Candidate status

| Item | Value |
|---|---|
| Baseline | Official v1.0.9, commit `0612c0acb500f096da5cfb766f6e7f0a012c4ba8`, code44 |
| Candidate | 1.0.9, proposed code45; Console uniqueness check pending |
| Android | Minimum API23; compile/target API36 |
| Updates | Google Play; no app APK self-installer |
| Fresh Settings Lock | OFF; Admin management and parental gates independent |
| Repository | Local preparation only; the owner manually creates/verifies/pushes any PRIVATE youriptvus/YourIPTV-GooglePlay remote |

See [source provenance](SOURCE_BASELINE.md), [build/testing](docs/PLAY_BUILD_AND_TEST.md), [submission package](GooglePlay-v1.0.9/PLAY_STORE_README.md) and [test report](GooglePlay-v1.0.9/PLAY_STORE_TEST_REPORT.md). Imported customer documents are historical reference; Play documents supersede customer-only installation, provider shortcut, lock-default and updater instructions.

## Player features

Live TV, categories, previews and integrated guide; fullscreen playback and favorites; provider-supported catch-up, Movies and Series; playlist/EPG refresh and saved preferences; parental restrictions and provider limits; import/export and supported recording/reminders. Right on a TV remote recalls the prior channel in the current fullscreen live queue, and a second press toggles back. Existing recovery guards and retry timing remain.

Recording uses a provider connection. Reminders and scheduled recording retain the app-running limitation. Background refresh depends on Android scheduling/network/power conditions. Providers control content and guide coverage. See [known issues](docs/KNOWN_ISSUES.md); historical tests do not certify this candidate.

## Setup and security

Enter your own provider URL, username/password where needed, playlist name and guide URL. Prefer HTTPS where supported; HTTP sources or provider redirects can expose credentials/content to the network. Keep playlists, credentials, private-account screenshots and backups private.

Fresh Play state opens normal Settings without a Settings PIN. Deliberate Settings Lock management remains Admin protected. Existing security state is preserved when freshness cannot be reliably established. Parental checks remain independent. See [security](docs/SETTINGS_SECURITY.md) for local Admin enrollment and legacy migration.

Credentials use app-private local storage; this is not database encryption. Exported JSON backups contain credentials. Automatic cloud backup is disabled, and explicit Android backup/device-transfer rules exclude the app's data. Manual export and compatible in-place updates retain their existing behavior. The offline in-app privacy notice explains data flows; owner must publish the matching privacy webpage before submission.

## Independent development

Build here without the customer source directory. Manually review future ports using [maintenance](docs/PLAY_MAINTENANCE.md); no automatic synchronization. Customer and Play apps share a package and normally cannot coexist; updates also require compatible signing and a higher code. Do not overwrite customer installs for QA.

Keys, passwords, SDK paths, test accounts and artifacts stay outside tracked source. Release signing reads external protected configuration via `YOURIPTV_UPLOAD_SIGNING_PROPERTIES`; no configuration means unsigned release output. Upload certificate and Play app-signing certificate have different roles. See [signing](GooglePlay-v1.0.9/PLAY_STORE_SIGNING.md).

The owner's latest instruction restricts this work to local operations. No GitHub creation, remote verification, push or upload is performed by workspace tools. The configured origin is retained for the owner's manual use; it does not prove a private repository exists. No application publication is performed.

## License

No source license has been selected by the owner. Free app functionality does not grant permission to redistribute source. Library licenses remain applicable. Use owner-approved artwork and authorized sample media for store assets.
