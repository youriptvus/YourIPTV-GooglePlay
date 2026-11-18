# Independent build and tests

> **Current status, November 17:** [Active Play 1.0.9 / proposed code 48](../GooglePlay-v1.0.9/CURRENT_STATUS.md) is owner-approved local preparation, pending unused Console-history verification. Code 45 artifacts are historical. No `.150` testing/reinstall without a new explicit owner request. The owner attributes the separate playback issue to the server; no encoder/decoder changes are included.

The current HTTPS-only secure candidate, production source `cc13ebe5bba6dfba1fd00d38e976ce2c1756e4f3`, is being prepared separately under `artifacts/code48-secure/`. Secure-source host results passed 414 JVM tests / 57 classes, focused 61 / 6 classes, verifier tests 6/6 and full lint (0 errors, 272 warnings); final signing/compiled-artifact results are pending. The 401-test HTTP-integrity build and first 81-check code 48 artifact audit predate this transport change and are intermediate/superseded. Root-level code 45 evidence remains historical. See [current secure status](../GooglePlay-v1.0.9/CURRENT_STATUS.md).

The intermediate HTTP-integrity source was Play 1.0.9 / proposed code 48, build commit `2c02256822cfc8c9c6b0436cceaeb1f4e7b3830a`; the current HTTPS-only source is a later snapshot. The encoded Content-Length change passed 401 JVM tests / 56 classes, full lint (0 errors, 272 warnings) and 16/16 synthetic localhost checks. These are host results; no new device/provider validation or TV EOF cause is established. New delivery files belong under `GooglePlay-v1.0.9/artifacts/code48-secure/`; the root-level code 45 AAB/APK/source ZIP remain historical and unchanged. Packaging/signature/artifact results are pending in [current status](../GooglePlay-v1.0.9/CURRENT_STATUS.md).

Run from D:\APK\Repository\YourIPTV-GooglePlay. Checked-in Gradle9.8.1/Android Gradle Plugin9.5.0, compatible JDK17+, SDK Platform36 and suitable Build Tools. Configure SDK through local environment or ignored local.properties; no dependency on customer source directory.

```powershell
cd D:\APK\Repository\YourIPTV-GooglePlay
.\gradlew.bat clean testReleaseUnitTest lintRelease bundleRelease assembleRelease
```

One production bundle: app\build\outputs\bundle\release\app-release.aab. Delivery name: YourIPTV-v1.0.9-GooglePlay.aab. Artifacts excluded from Git. Without external signing configuration outputs unsigned, not upload-ready. To sign, set YOURIPTV_UPLOAD_SIGNING_PROPERTIES to protected properties outside project; keystore also outside project. Never print secrets; see signing document.

Verify the artifact-verifier regression tests locally:

```powershell
py -m unittest discover -s scripts/tests -p test_verify_play_candidate.py
```

Inspect unit report app\build\reports\tests\testReleaseUnitTest\index.html, full lint reports and final merged manifest/bundle metadata. Commands above are instructions; actual outcomes belong in test report. lintVitalRelease alone is not a full lintRelease pass.

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath app\build\outputs\bundle\release\app-release.aab
```

Verify package/version/SDK, launcher intents, optional touchscreen, TV banner/icons, exported components, no installer/update-overlay permissions or executable updater path/APK payload and all native libraries. Source manifest alone is insufficient.

## Isolated device tests

Historical targets were TV192.168.1.150:5555 (onn.4K Pro) and phoneR5GL75PGMFP (Samsung). The owner later removed both app packages from `.150`; do not resume player testing or reinstall there without a new explicit owner request. The following procedures require that authorization before use. After authorization, read model/manufacturer/API and installed package/version/signing before any install. Never overwrite customer com.youriptv.app. Temporary `.playtest` package shares the source, not a second production release.

```powershell
.\gradlew.bat -PisolatedDeviceTests=true assembleIsolatedTest assembleIsolatedTestAndroidTest
```

Only use actual implemented tasks; verify Gradle task/output names before device install. Isolated tests cannot prove production migration/signing/Play delivery. Never uninstall/clear customer data, alter PINs/playlists/launcher or factory reset. Sound assumed unless actual problem.

Cover fresh Settings OFF/restart, authorized On/Off/relock, Admin/parental independence, limits, legal-source guide/catalogue refresh, remote/touch/fullscreen/system bars/keyboard, Right history, TS clean-end/retries, switching and lifecycle. Retain failures/unavailable devices. ADB connection/install is not functionality. Add prolonged playback/repeated interruptions and actual Play-generated updates on an authorized internal track before final launch. JVM/source assertions are not on-device playback tests.

## Independent CI workflow

`.github/workflows/verify.yml` runs the unified unsigned production build/tests/full lint on Ubuntu for manual dispatch, pull requests and main/development pushes. The inspected workflow uses commit-SHA-pinned checkout/setup-java/upload-artifact actions, contents:read, checkout persist-credentials:false, Java21,30-minute timeout and14-day report/unsigned-bundle artifact retention. It contains no signing secrets, customer release/update hooks or Play publication step.

This is a source-independent workflow: it checks out only this repository and runs its own Gradle wrapper. No successful remote Actions run is claimed before authenticated remote setup/push. Review the actual runner SDK availability and any first-run warnings; a checked-in workflow is not CI execution evidence.
