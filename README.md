# YourIPTV for Google Play

YourIPTV is an independent IPTV media player for Android phones, tablets, Android TV and Google TV. Add an authorized M3U/M3U Plus playlist, Xtream Codes account and XMLTV guide. The app supplies no channels, account or subscription. One production package supports phones and TV.

Website: [youriptv.us](https://youriptv.us) · Support/privacy: `admin@youriptv.us` · Public privacy policy: [youriptv.us/privacy](https://youriptv.us/privacy)

## Current build

| Item | Value |
|---|---|
| Project version | **1.1.1 / versionCode 52** |
| Confirmed published baseline | **1.1.0 / versionCode 51**; unchanged by this local preparation |
| Application ID | `com.youriptv.app` |
| Android | Minimum API24 / Android7.0; compile and target API36 |
| Delivery | Google Play App Signing and Play-managed updates; no direct APK self-installer |
| Protection | Preserve the existing automatic-protection and signing configuration |
| Status | Unpublished local candidate; this preparation does not upload to Console or publish a release |
| Current release records | [GooglePlay-v1.1.1](GooglePlay-v1.1.1/RELEASE_SUMMARY.md) and its artifact hashes/publication instructions |

The customer distribution is maintained separately with minimum API23 and its own APK updater. The production packages share the same application ID and cannot normally coexist; installation/update compatibility also depends on signing and versionCode. Use the isolated `.playtest` package for diagnostic device checks rather than replacing an existing production installation.

## Features in 1.1.1

- **Series history** displays one parent series entry with artwork and the last watched episode. Selecting it opens seasons and per-episode progress. Movies remain separate history entries. Continue prioritizes the most recently watched unfinished episode or the next available episode after completion.
- **Green episode bars** show partial and completed playback; unwatched episodes have no progress line. Resume, Play From Start, manual episode selection and Continue Watching use the same stored positions.
- **Autoplay Next Episode** defaults ON only when no saved choice exists; saved OFF remains OFF. The ordinary Playback setting requires no Admin PIN. Confirmed completion advances after a cancellable countdown through available episodes and later seasons. With autoplay OFF, or after the final available episode, playback returns without looping. Manual Next remains available.
- **Manual progress actions win:** watched, unwatched and clear choices cannot be overwritten by delayed checkpoints from an older playback session. Fresh intentional playback can track again. Captured observation times and same-session ordering guards keep older snapshots from regressing newer progress. This amendment retains existing history without a destructive migration.
- **Live TV and catch-up** retain categories, favorites, preview/fullscreen playback, guide details, source-supported archives, reminders and recording. Catch-up opens the archive player with its address and archive flag intact.
- **Navigation** includes held physical Back to return to the current stream, ordinary short Back navigation, remembered live-channel launcher resume from any screen, and explicit guide opening on the requested source/channel at the current timeslot. Background guide refresh does not reset active past/future browsing. TV D-pad and phone touch layouts share the same playback/data implementation.
- **Source compatibility** preserves explicit user-selected HTTP or HTTPS for playlists, Xtream, guides, artwork and media. HTTPS certificate validation and HTTPS-to-HTTP redirect blocking remain enabled. Existing saved source schemes are not silently rewritten.

Other standard features include multiple playlists, provider-account details and connection-limit handling, catalogue/EPG refresh, movie details, season/episode browsing, favorites/search, subtitle/audio/video selection, aspect ratio, supported picture-in-picture, an external player, buffer/decoder preferences, audio delay, optional frame-rate matching, and manual backup/restore. Player features are free. Providers control content, metadata, guide/archive availability and account limits.

## Setup and security

Open Playlists and enter the provider's exact URL/scheme/port, username/password where needed, playlist name and optional guide URL/User-Agent. Generic M3U and Xtream setup are available; no preset customer-provider account is included. A `get.php` link can be imported as Xtream or retained as M3U. Use only sources you are authorized to access.

Settings Lock defaults OFF for fresh/unconfigured state, while saved local choices, prior Settings PIN protection and ambiguous existing protection remain preserved. Admin authorization for lock/PIN management and parental checks remain independent. Fresh Play installations enroll their local Admin PIN when using protected Admin management. Normal Playback controls, including autoplay, remain available without the Admin PIN. See [Settings security](docs/SETTINGS_SECURITY.md).

Credentials/history use local app-private storage, not additional database encryption. Exported JSON backups may contain readable provider credentials and addresses. Keep source logins, private screenshots and backups out of committed source and shared diagnostics. PIN secrets and the local Settings Lock choice are excluded from portable exports. Automatic system cloud backup is disabled and explicit backup/device-transfer exclusions remain. Compatible in-place updates retain app data; do not uninstall or clear production storage for QA.

HTTP requests can include source credentials and are not encrypted in transit. Prefer a working HTTPS source where the provider supports it. Before submission, publish the matching privacy-policy replacement and review Data safety against actual data transfers, including hosting practices. Do not claim all covered data is encrypted if covered information can travel over HTTP. See [publication requirements](GooglePlay-v1.1.1/PUBLICATION-REQUIREMENTS.md).

## Build on Windows

Build this independent project directly. Do not copy the customer build configuration over Play-specific signing, delivery, backup, minimum-SDK or security settings.

The maintainer's active local Play checkout is `D:\APK\Repository\YourIPTV-GooglePlay`. The separate customer owner source is `D:\APK\Repository\YourIPTV`; its private build/test project is `D:\APK\work\YourIPTV-vod-redesign-customer`. Build and test each explicit project, preserving its signing/distribution configuration. All Play signing properties and keys remain outside the Play checkout.

| Requirement | Project configuration |
|---|---|
| JDK | JDK21 used for current local verification; Java17 language/bytecode target |
| Android SDK | Platform36, Build-Tools36.0.0 and Platform Tools |
| Gradle | Pinned wrapper9.8.1; no separate Gradle install |
| Android Gradle Plugin | Pinned9.5.0 with built-in Kotlin |

The checked-in wrapper/build files define versions. Dependencies must be available from the configured repositories or already cached for offline work.

```powershell
cd <YourIPTV-GooglePlay-checkout>
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
.\gradlew.bat assembleDebug
.\gradlew.bat testReleaseUnitTest assembleRelease bundleRelease lintRelease
```

An ignored `local.properties` can supply `sdk.dir` instead. Release output is under `app\build\outputs\apk\release` and `app\build\outputs\bundle\release`; unit/lint reports are under `app\build\reports`. A local APK is supplemental diagnostic material; Google Play distribution uses the reviewed AAB and Play-generated APKs.

### External signing

Release signing reads `YOURIPTV_UPLOAD_SIGNING_PROPERTIES`. Set it to the existing protected properties file outside the repository:

```powershell
$env:YOURIPTV_UPLOAD_SIGNING_PROPERTIES = "C:\private\youriptv-upload.properties"
.\gradlew.bat assembleRelease bundleRelease
```

The external file contains `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Both that properties file and its key must remain outside the repository. Relative key paths resolve from the external properties file's directory. No signing configuration means unsigned release output. Never commit/copy private keys, passwords, local test settings or signing exports into source/archive files, create replacement production keys, or change Play signing enrollment as a build workaround.

The upload certificate and Play app-signing certificate have different roles. Preserve the existing signing configuration and automatic protection. A debug/isolated APK cannot replace an officially signed production installation. A signed local build is not evidence that the protected Play-delivered application has been tested.

### Isolated device tests

The diagnostic build uses `com.youriptv.app.playtest`; it does not create another production phone/TV product.

```powershell
.\gradlew.bat -PisolatedDeviceTests=true assembleIsolatedTest assembleIsolatedTestAndroidTest
adb devices
```

Install/run only the intended isolated APK and selected instrumentation on an authorized device. Keep owner production data, playlists, credentials, PINs and unrelated apps intact. Generated fixtures and failure evidence stay outside committed release source. See the final release records for the exact tests, artifacts, device coverage and remaining limits.

## Verification and maintenance

Use the shared feature comparison script when porting functionality:

```powershell
py scripts/check_shared_vod.py --other-repo <customer-project>
```

Run relevant unit/native/UI checks and review the actual compiled APK/AAB, signatures, package/SDK/permissions, backup/network rules and known-private-file/content scans. Keep failures and superseded candidates as historical evidence. Final counts and SHA-256 values belong to the matching release's summary, `SHA256SUMS.txt`, publication instructions and artifact-bound verification sidecars; this README does not invent a final metric or embed its own commit hash.

Scope remains local until the owner deliberately commits/pushes and publishes. Workspace tools do not call GitHub, upload to Play or deploy website files. Future maintenance ports must preserve Play-specific configuration and review privacy and distribution requirements separately. See [maintenance notes](docs/PLAY_MAINTENANCE.md); dated older candidate records remain historical.

## Manual publication gates

1. Finish the final checks and freeze the exact reviewed AAB/source. Verify that code52 is unused in the complete Console upload history, including all tracks, drafts and retired uploads. If it is already consumed, stop for a versionCode decision while retaining the requested versionName1.1.1. The updater does not offer a different customer APK with the same code, and Google Play requires an unused upload code.
2. Publish the matching privacy policy at the public URL and review Data safety, reviewer access, store assets, app-content declarations and current country/account requirements. HTTP support does not remove User Data policy obligations. Follow [1.1.1 publication requirements](GooglePlay-v1.1.1/PUBLICATION-REQUIREMENTS.md).
3. Keep the existing app entry, signing and automatic protection. Preserve the current testing track and real tester enrollment/continuity. Check Console's current closed-testing and production-access gates rather than treating older counts/dates as current eligibility.
4. The owner deliberately uploads the reviewed AAB, checks Play processing/pre-launch findings and tests the delivered protected phone/TV build before the intended rollout. Passing local checks does not guarantee Google acceptance, production access or uninterrupted playback.

Customer GitHub publication remains separate. Preserve the development-local original `v1.1.1` tag and the reviewed amendment tag. The owner must manually confirm the remote canonical `v1.1.1` tag/release is unused before creating it on the verified final owner-source commit; stop if it already exists. Do not push old development tags wholesale or force-update a tag. This keeps the canonical customer updater route and automatically generated release source truthful without changing Play delivery.

## Limitations and documentation

Providers can omit guide/artwork/details, reject streams or impose connection limits. Recording uses a provider connection; scheduled reminders/recording retain their app-running limitation. Android scheduling, network/power settings, codecs, system GUIDE keys, storage, picture-in-picture and refresh modes vary by device. Scoped generated-fixture tests do not establish every provider/device combination or a long-duration soak.

See [release notes](docs/RELEASE_NOTES.md), [VOD navigation](docs/VOD_NAVIGATION.md), [progress behavior](docs/VOD_PROGRESS.md), [remote controls](docs/REMOTE_CONTROLS.md), [known issues](docs/KNOWN_ISSUES.md), [Settings security](docs/SETTINGS_SECURITY.md) and the current [release summary](GooglePlay-v1.1.1/RELEASE_SUMMARY.md). Historical source-import and Console handoffs describe their dated candidates, not the current 1.1.1 candidate's validation or publication status.

## License

No source license has been selected by the owner. Free app functionality does not grant permission to redistribute source. Dependency licenses remain applicable. Use owner-approved artwork and authorized sample media for store assets.
