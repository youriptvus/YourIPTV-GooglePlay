# Google Play build and test guidance

Current local candidate: **YourIPTV 1.1.1 / versionCode 52**, package `com.youriptv.app`, minimum API 24, target/compile API 36. One production application and AAB support phones, tablets and Android TV. This page describes local preparation, not a Play upload or production approval.

The Play project is independent from the customer project. It retains Play-managed updates and has no direct APK downloader/installer. Existing signing and automatic Play protection remain unchanged. User-entered HTTP and HTTPS sources are supported; explicit schemes are preserved, platform HTTPS certificate validation remains enabled, and HTTPS-to-HTTP downgrade redirects remain blocked.

## Build locally

Use the checked-in Gradle wrapper with Java 17 and the configured Android SDK. Release signing uses `YOURIPTV_UPLOAD_SIGNING_PROPERTIES`, pointing to an existing properties file outside the repository; its referenced signing key must also remain outside the repository. Never print, copy into source, archive or replace private signing material.

From this project directory, the release checks/build tasks are:

```powershell
.\gradlew.bat --no-daemon testReleaseUnitTest lintRelease assembleRelease bundleRelease
```

Keep build logs and parsed test results alongside the delivery. Inspect the actual APK/AAB for package, version, SDKs, certificates, permissions, transport configuration, absence of self-installation routes and physical Back-key routing. Use the existing artifact-verification scripts according to their `--help`; do not substitute a source-only scan for inspecting compiled artifacts. Record exact hashes and source commit in refreshed artifact sidecars after the final build. Historical sidecars do not certify a changed candidate.

Device tests use the temporary `com.youriptv.app.playtest` isolated package. Build its app and test APKs with `-PisolatedDeviceTests=true` and the `assembleIsolatedTest` / `assembleIsolatedTestAndroidTest` tasks. Preserve existing production apps, playlists, PINs and provider accounts; never clear production data to make a test pass.

## Verified device scope on November 21, 2026

The final isolated-package record reports **185 passing checks**: 93 on Moto g (2025), API 36, and 92 on onn. Streaming Device 4K Pro, API 34. These cover Settings defaults, native progress/session ordering, VOD/series UI and playback, catch-up fixtures, source/account guards, guide entry/current-time/focus, and held Back with release survival. Both devices' production application identities were preserved.

Evidence: `D:\APK\work\YourIPTV-history-device-qa-20261121T163606Z\watch-guard-final\final-summary.json`. It binds the isolated app APK SHA-256 `02afe9ef2c7956258bf7501d53b7da6fc87dc14515ef61c1344ddf7dfcc13fc0` and each suite's test-APK provenance. Moto VOD results use the recorded foreground-ready rerun on the same app binary; superseded failures remain retained. These are device checks, not JVM counts or a production AAB hash.

Samsung authorization was unavailable. No private provider or reference-series catalog was configured for these tests; generated video was actually decoded. Sound was assumed. Tablet, prolonged provider playback and actual protected Play delivery of code 52 remain outside this result. Obtain final host/JVM/lint totals and release APK/AAB identities from their final build sidecars; do not reuse older candidate counts.

Before publication, complete [PUBLICATION-REQUIREMENTS.md](../GooglePlay-v1.1.1/PUBLICATION-REQUIREMENTS.md), including unused-code verification, accurate HTTP privacy/Data safety wording and real Play-delivered track tests. Earlier build instructions are preserved in [the dated archive](history/2026-11-21-pre-1.1.1-refresh/PLAY_BUILD_AND_TEST.md).
