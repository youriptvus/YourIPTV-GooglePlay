# Current local Google Play compliance review

Reviewed November 18, 2026. Local engineering checks passed for 1.0.9 / proposed code 48. This report does not establish Google approval; owner-specific declarations and Play validation remain.

| Area | Current evidence | External limit |
|---|---|---|
| Identity/form factors | One com.youriptv.app AAB, shared phone/tablet/TV code and launcher declarations | Console ownership/history and device catalog |
| API/toolchain | compile/target 36, min23; successful signed build with pinned AGP9.5/Gradle9.8.1/Media3 1.11.1 | Recheck policy at upload if delayed |
| Updates | APK self-updater/downloader/installer paths and installer/overlay permissions removed | Actual Play-generated update test |
| Security | Fresh Settings OFF, independent local Admin/parental/lockouts, provider limits, protected migrations and backup/transfer exclusions | No arbitrary security/unknown-secret certification |
| Transport/data | Compiled cleartext/downgrade denial; platform TLS; generic HTTPS sources; known-token/QA separation passed | Third-party recipient practices and manual export handling |
| Privacy | Public policy reachable, 29 prepared text blocks matched; admin@youriptv.us | Keep policy/mailbox operational; owner saves Data safety |
| Content/listing | Free player, no default provider/content/paywall/ads; legal generated reviewer source tested | Accurate intended audience/IARC and source-restricted access |
| TV/native | Correct banners/icons and launchers; no packaged native .so; D-pad/RIGHT/player suites passed | Physical 16 KB/remote timing and Google TV review unperformed |
| Accessibility | Google ATF LATEST zero errors on seven audited screens per device; actual keyboard focus | Warnings retained; manual TalkBack/switch access/all screens untested |
| Build/tests | 414 JVM, six verifier tests; lint zero errors/266 warnings; phone35/TV32 and public14 each passed | Local isolated QA differs from production/Play delivery |

Source/compiled implementation and exact outcomes: Build-Report.md, Test-Report.md, Accessibility-and-Public-Playback.md, Artifact-Audit.json and Test-Separation-Audit.json. See Final-Delivery-Report.md and START-HERE.md for all remaining owner actions. Earlier failed attempts and superseded binaries are preserved outside the active delivery.

Current primary requirements checked: [target API](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en), [TV quality](https://developer.android.com/develop/adaptive-apps/quality-guidelines/tv-app-quality), [User Data/privacy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en), [Data safety](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en), [account testing](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en), [signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en). API36 satisfies the current unified/mobile target requirement. Newer personal accounts covered by the testing policy need 12 continuously opted-in testers for 14 days before applying for production access. Existing account registration is not required again.
