# Independent build and tests

Run from D:\APK\Repository\YourIPTV-GooglePlay. Checked-in Gradle9.8.1/Android Gradle Plugin9.5.0, compatible JDK17+, SDK Platform36 and suitable Build Tools. Configure SDK through local environment or ignored local.properties; no dependency on customer source directory.

```powershell
cd D:\APK\Repository\YourIPTV-GooglePlay
.\gradlew.bat clean testReleaseUnitTest lintRelease bundleRelease assembleRelease
```

One production bundle: app\build\outputs\bundle\release\app-release.aab. Delivery name: YourIPTV-v1.0.9-GooglePlay.aab. Artifacts excluded from Git. Without external signing configuration outputs unsigned, not upload-ready. To sign, set YOURIPTV_UPLOAD_SIGNING_PROPERTIES to protected properties outside project; keystore also outside project. Never print secrets; see signing document.

Inspect unit report app\build\reports\tests\testReleaseUnitTest\index.html, full lint reports and final merged manifest/bundle metadata. Commands above are instructions; actual outcomes belong in test report. lintVitalRelease alone is not a full lintRelease pass.

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath app\build\outputs\bundle\release\app-release.aab
```

Verify package/version/SDK, launcher intents, optional touchscreen, TV banner/icons, exported components, no installer/update-overlay permissions or executable updater path/APK payload and all native libraries. Source manifest alone is insufficient.

## Isolated device tests

TV192.168.1.150:5555 expected onn.4K Pro; phoneR5GL75PGMFP expected Samsung. Read model/manufacturer/API and installed package/version/signing before install. Never overwrite customer com.youriptv.app. Temporary `.playtest` package shares the source, not a second production release.

```powershell
.\gradlew.bat -PisolatedDeviceTests=true assembleIsolatedTest assembleIsolatedTestAndroidTest
```

Only use actual implemented tasks; verify Gradle task/output names before device install. Isolated tests cannot prove production migration/signing/Play delivery. Never uninstall/clear customer data, alter PINs/playlists/launcher or factory reset. Sound assumed unless actual problem.

Cover fresh Settings OFF/restart, authorized On/Off/relock, Admin/parental independence, limits, legal-source guide/catalogue refresh, remote/touch/fullscreen/system bars/keyboard, Right history, TS clean-end/retries, switching and lifecycle. Retain failures/unavailable devices. ADB connection/install is not functionality. Add prolonged playback/repeated interruptions and actual Play-generated updates on an authorized internal track before final launch. JVM/source assertions are not on-device playback tests.

## Independent CI workflow

`.github/workflows/verify.yml` runs the unified unsigned production build/tests/full lint on Ubuntu for manual dispatch, pull requests and main/development pushes. The inspected workflow uses commit-SHA-pinned checkout/setup-java/upload-artifact actions, contents:read, checkout persist-credentials:false, Java21,30-minute timeout and14-day report/unsigned-bundle artifact retention. It contains no signing secrets, customer release/update hooks or Play publication step.

This is a source-independent workflow: it checks out only this repository and runs its own Gradle wrapper. No successful remote Actions run is claimed before authenticated remote setup/push. Review the actual runner SDK availability and any first-run warnings; a checked-in workflow is not CI execution evidence.
