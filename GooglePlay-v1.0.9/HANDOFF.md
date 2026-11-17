# YourIPTV Google Play release-candidate handoff

Prepared November 16, 2026 (America/New_York). The local release candidate is built and verified to the scopes below. Retained failures and remaining owner gates are explicit. The owner's latest instruction permits local work only; no GitHub creation/verification/push/upload is performed by workspace tools. No GitHub Release, Play Console upload, signing enrollment, publication or Google approval is claimed.

| Status | Delivery scope |
|---|---|
| Completed and verified | Independent local source/history; signed unified AAB/APK; 385 host tests; full lint; 77 artifact checks; qualified TV/phone synthetic tests, real restart/replacement, and bounded real-provider decoding |
| Implemented with limited runtime coverage | Inherited player features remain; actual VOD/Series/catch-up/recording/reminders, older Android/tablet/16KB hardware, prolonged playback and production/Play-generated upgrades were not exhaustively exercised |
| Requires owner action | Manual PRIVATE GitHub setup if desired; Console code history/signing/account/testing; legal privacy/contact/declarations; final store assets/public reviewer access; explicit submission approval |
| Unresolved/retained failures | Full 40 MB private catalog on TV truncated with EOF; one private TV stream stalled before another passed; three original Git fetch/tag metadata differences have unconfirmed cause |

## Project and provenance

| Delivery item | Record |
|---|---|
| Original customer repository | https://github.com/youriptvus/YourIPTV |
| Intended new PRIVATE repository | https://github.com/youriptvus/YourIPTV-GooglePlay; remote existence, visibility and push are not yet verified |
| Independent local project | `D:\APK\Repository\YourIPTV-GooglePlay` |
| Verified official source | `v1.0.9`, commit `0612c0acb500f096da5cfb766f6e7f0a012c4ba8`; public latest release, tag and main matched at import |
| Source version | `com.youriptv.app`, versionName `1.0.9`, versionCode `44` |
| Initial independent commit | `1c633e5d6ac08203b76b3f13434c00b214c7123c` — `Initial Google Play project imported from YourIPTV v1.0.9` |
| Production build commit | `f5a421823aa94fc8f652edc52050b8f12f5d7130`; the verified AAB/APK below were built from this committed production source |
| Local branches | Independent `main` and `development`; no original Git directory, worktree, branch or automatic synchronization |
| Only configured remote | `origin` fetch/push: `https://github.com/youriptvus/YourIPTV-GooglePlay` |
| Candidate version | versionName `1.0.9`, proposed versionCode `45`; not verified unused in Play Console |
| Production identity | One `com.youriptv.app` application, shared player and production AAB for phone, tablet, Android TV and Google TV |
| Platform | min SDK 23, compile SDK 36, target SDK 36; Java 17 bytecode, JDK 21 used locally |

Source provenance and intentional differences are in [SOURCE_BASELINE.md](SOURCE_BASELINE.md). The [pinned 234-file inventory](SOURCE_FILE_INVENTORY.json) records original blob digests without copying private configuration. The initial import commit includes the sanitized Play changes, so no plaintext legacy Admin credential enters its history.

A separate nested checkout appeared at `YourIPTV-GooglePlay/` inside the new project during preparation. The owner did not identify it as their creation. It is preserved in place, ignored and excluded from this project's source and artifact history. Its initial commit does not establish remote ownership or privacy. Do not overwrite any existing remote history.

## Implemented behavior

| Area | Result and verification scope |
|---|---|
| Settings Lock default | Reliably fresh Play preferences persist OFF once; normal Settings requires no Settings PIN. Host policy and isolated TV/phone checks pass. |
| Existing security | Explicit saved ON/OFF is retained. Existing preferences without reliable lock provenance conservatively retain ON. No default-enabled lock is silently inferred to be owner-disabled. |
| Admin | Local salted Admin PIN enrollment replaces the universal fresh-install master. Management requires fresh Admin authentication and persisted Settings lockouts. A digest-only legacy verifier is restricted to migrated state until authenticated local enrollment. |
| Parental controls | Existing parental PIN, restrictions and failed-attempt counters remain separate from Settings Lock and Admin. Custom Settings access cannot grant Admin authority. |
| Automatic backup/transfer | Cloud backup remains disabled. Complete system backup/D2D exclusions cover normal and device-protected data; manual exports and compatible in-place updates are unchanged. Host audit and actual compiled AAB/APK resource checks pass. |
| APK updater | Direct update downloader/checker/install activity and customer update BuildConfig routes removed. Installer, silent-update and overlay permissions removed. Google Play delivers updates. |
| Update UI/privacy | About and update text describe Play. Public About opens an offline bundled privacy notice on both input types; no TV browser dependency. |
| Public provider setup | Customer subscription-provider shortcut removed. Generic Xtream, M3U/M3U Plus and XMLTV entry retained. Scheme-less Xtream input prefers HTTPS; explicit HTTP requires confirmation. |
| Shared player | Baseline Media3 playback and recovery implementation retained. Touch controls, responsive dimensions, rotation and system-bar/keyboard insets adapt the shared UI. |
| v1.0.9 features | Faster first clean-ended fullscreen live TS recovery, later retry/backoff and switching guards, RIGHT previous-channel toggle, Info label, parental rules and connection limits retained. |
| Refresh/features | Existing playlist interval/startup EPG behavior, guide, previews, favorites, provider-supported catch-up/VOD/Series, import/export, and supported recording/reminders retained. No paywall, premium gate, trial or playback timer added. |
| Independence | Builds use this source directory and its Gradle configuration. Signing is optional external configuration, not an implicit dependency on the customer source. Prepared CI is configured for unsigned verification with read-only permissions and no publishing step; it has not run remotely. |

Recording/reminders retain their existing app-running limitations. Provider content, guide availability and account limits still apply. Preserving source behavior does not prove every feature on every device.

## Artifacts, signing and tests

The single verified production bundle is `D:\APK\Repository\YourIPTV-GooglePlay\GooglePlay-v1.0.9\artifacts\YourIPTV-v1.0.9-GooglePlay.aab`. The supplemental APK is for controlled release verification, not a substitute for the Play AAB. A temporary `.playtest` package protects customer installations during device tests; it is not a second production application, listing or AAB.

| Local delivery | Bytes | SHA-256 |
|---|---:|---|
| `YourIPTV-v1.0.9-GooglePlay.aab` | 5,508,849 | `26c0597e3616f37c779be477a31423ba4b5199348e981f2754c1b65c2d5c5d12` |
| `YourIPTV-v1.0.9-GooglePlay.apk` | 3,743,910 | `319737af5691bd7138814398e368d118e8df15c9b9e8a302b11d503ee24ce9d3` |

Existing official public app-signing certificate SHA-256:

`4aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539`

The local build reads the existing protected signing properties/key externally. No production key was created, replaced, copied into this repository or enrolled with Play. An official-signed local bundle alone cannot establish the certificate used on future Play-generated APKs. The runtime certificate gate also requires compatible Play signing. See [signing](PLAY_STORE_SIGNING.md).

| Verification | Current record |
|---|---|
| Release unit tests | **385 tests / 55 classes passed; 0 failures, errors or skipped tests** |
| Full `lintRelease` | **Passed with 0 errors and 272 warnings**. Warnings remain documented; this is not a warning-free or blanket policy-approval claim. |
| Lint corrections | Media3 opt-in annotations, two narrowly documented dialog-authorization false positives and shared AndroidX Back callback handling; no blanket lint baseline |
| Release assembly/AAB | Final offline release packaging passed. Earlier pre-exclusion hashes are intermediate and superseded. Clean build occurred before corrections. |
| Final AAB/APK audit | **77 checks passed; 0 failed checks/errors**. Package/version/API, permissions, both launchers/optional hardware, icons/banner, actual compiled backup resources and official signatures verified. No packaged native `.so`, APK installer path, legacy plaintext credential or test markers. |
| Local repository independence | **8 checks passed**: own Git directory/history, no alternates/attached original worktree, new-only origin, one app module and no production flavors |
| Settings OFF/choice persistence/migration/Admin/parental/limits | Host tests and both devices' scoped security/UI checks pass; legacy inputs are synthetic, not a production customer upgrade |
| onn. TV | Connected: `192.168.1.150:5555`, Android API 34, customer `1.0.9` / code 44; no customer replacement authorized |
| Samsung phone | Connected: `R5GL75PGMFP`, `SM-S166V`, Android API 36, customer `1.0.3` / code 34; no customer replacement authorized |
| Live/guide/refresh/RIGHT/recovery | TV full run 26/27 plus focused 1/1; Samsung full run 28/30 plus focused 2/2. All scoped scenarios have evidence across runs; no single full 27/27 or 30/30 pass is claimed. |
| Bounded playback observation | Both 180-second soaks passed with policy-derived expectations, 5 ends / 4 new first-frame events / 8 GETs / 0 player errors each. Max observed gap: TV 32.548 s, phone 32.194 s. Later baseline backoff is preserved; prior harness failures remain in the report. |
| Restart and compatible replacement | Both devices' actual process restart and same-code test-APK replacement passed for OFF and active playlist ID; customer installation metadata unchanged. This is not a versionCode-increasing production upgrade test. |
| Private provider trial | Phone full 147,803-item catalog/53,030-program guide + 60.141 s actual 720p H.264/MPEG-TS passed. TV three-entry subset + direct 60.099 s actual 720p H.264/MPEG-TS candidate 2 passed; candidate 1 stalled and full 40 MB TV import still failed EOF. No full TV private catalog/EPG pass is claimed. Cleanup/account-idle/customer-preservation passed. |
| Screenshots | 14 raw actual TV/phone captures visually reviewed; public/synthetic content only, no bitmap editing. [Capture index](screenshots/README.md). These are QA evidence; final production/store format approval and tablet captures remain owner work. |
| Original preservation | Tracked source, HEAD/index/config/local branches match the starting evidence. Three fetch/tag metadata changes were observed; strict all-Git-metadata preservation did not pass. See the qualification below. |
| Customer release APKs | Versioned 1.0.9, generic current and backup copies all match the owner-recorded official code 44 APK: 3,749,548 bytes, SHA-256 `6ac5ae11b0771ffd47c182a31318ff26ceaec53e7a3598110597af2c7a98b8fa`. Read-only checks; no artifact writes. |
| Remote PRIVATE/non-fork/push | Owner-manual under the latest local-only instruction; not performed or verified by workspace tools |

The detailed scenario matrix belongs in [PLAY_STORE_TEST_REPORT.md](PLAY_STORE_TEST_REPORT.md). Do not treat source assertions, constructor reconstruction, ADB connectivity or an isolated package as production migration/Play delivery evidence. Sound is assumed. Abrupt provider/network failures, long endurance, physical remote/audio timing, tablet hardware, accessibility and actual Play-generated updates require evidence before any pass is claimed.

## Exact build and verification commands

Run from the independent project. SDK and Java paths are local tooling, not source dependencies. The signing properties contain secrets and must never be printed or committed.

```powershell
cd D:\APK\Repository\YourIPTV-GooglePlay
$env:ANDROID_HOME='D:\AndroidDev\Sdk'
$env:JAVA_HOME='D:\AndroidDev\jdk-21'
$env:YOURIPTV_UPLOAD_SIGNING_PROPERTIES='D:\APK\release-signing\keystore.properties'
.\gradlew.bat clean testReleaseUnitTest lintRelease bundleRelease assembleRelease assembleIsolatedTest --console=plain --max-workers=2
.\gradlew.bat testReleaseUnitTest lintRelease --offline --console=plain --max-workers=1
.\gradlew.bat bundleRelease assembleRelease --offline --console=plain --max-workers=1
.\gradlew.bat -PisolatedDeviceTests=true assembleIsolatedTest assembleIsolatedTestAndroidTest --offline --console=plain --max-workers=1
```

The first command records the earlier clean-build stage. Corrections were recompiled and verified by the later one-worker host and packaging commands; the checked-in JVM heap is 2 GB. Without the signing environment variable, production output is unsigned. Device testing uses only the validated `.playtest` APKs. [Build/test instructions](../docs/PLAY_BUILD_AND_TEST.md) and checked-in verification scripts describe the exact safety checks.

## Policy findings and remaining owner actions

Current official requirements were researched and rechecked on November 16, 2026. The retained API 36 target meets the [current mobile submission requirement](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en); the unified app uses that requirement rather than the lower TV-only exception. Normal Play delivery avoids relying on an [in-app update API that does not support Android TV](https://developer.android.com/guide/playcore/in-app-updates). See the [source/manifest policy audit](../docs/PLAY_POLICY_AUDIT.md) and [official-requirements research](PLAY_POLICY_AUDIT.md).

Remaining owner actions:

1. If desired, manually authenticate as `youriptvus`, confirm any existing new remote/history, create it PRIVATE only if genuinely absent, verify PRIVATE/non-fork and safely push this independent history. Workspace tools must not execute those steps under the current local-only instruction. Never force-push unexpected history. [Owner-run remote steps](REPOSITORY_SEPARATION.md).
2. Check `com.youriptv.app` ownership, every uploaded Play versionCode and current customer release codes. Manually coordinate code allocation across both distributions without source synchronization. Approve 45 only if unused and above the installations meant to update; if a customer release reaches 45 or higher first, recheck and choose an unused higher code, then rebuild, retest and re-hash.
3. Decide compatible Play App Signing enrollment and upload-key handling. Preserve the official app-signing identity or approve a verified compatible lineage; no automatic key generation/enrollment.
4. Provide legal developer identity, monitored privacy/support contact, a public privacy webpage and final Data safety declarations based on actual transfers and organizational practices.
5. Host stable authorized reviewer content/access outside the home LAN. The generated local fixture proves no public availability. Complete content rating, ads and target-audience declarations.
6. Approve required artwork and actual final production screenshots; validate TV distribution, device catalog, phone/tablet quality and remaining accessibility/runtime cases.
7. Authorize Play internal testing, validate generated APK signatures and compatible data-preserving upgrades. Complete any required closed test and apply for production access; do not assume it is approved.
8. Explicitly authorize submission/publication when the gates are complete. No Google approval has been granted by this local work.

For newly registered personal accounts covered by the current rule, at least 12 testers must remain opted in continuously for 14 days before applying for production access. Consult the actual account dashboard and [official requirement](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en).

## Known limits and unresolved risks

- Shared package identity means customer and Play production installations normally cannot coexist. Higher versionCode plus compatible signing is required for updates; Play enrollment is unverified.
- User-selected sources and provider redirects can use HTTP. Initial HTTPS does not guarantee that all redirected traffic stays encrypted. Do not declare all data encrypted in transit.
- Provider credentials and credential-bearing URLs remain readable in app-private SQLite; explicit JSON exports contain credentials. PIN hashing is not database encryption. Protect backups and support evidence.
- Known Xtream connection guards remain preserved. An arbitrary M3U can have an unknown app-effective connection limit (0); the private trial's server reported 1 and only one actual stream was exercised. No blanket client-enforced 1 limit is claimed.
- Full private TV catalog download remains unresolved: the 40,346,015-byte response truncated with EOF after 16.127 s. A bounded three-entry metadata subset and direct provider stream worked, but full TV private EPG was not repeated. One TV candidate stalled before a second passed. Source/transport-specific cause is unproven, including any VPN attribution.
- A migrated installation retains the legacy master limitation until its owner authenticates and enrolls local Admin. A forgotten local Admin PIN has no unauthenticated bypass/reset path.
- No real customer credentials, provider account, customer database or signing secret is part of this import. Pattern-based scanning cannot prove the absence of every unknown secret; review before pushing.
- Isolated tests cannot certify the production package, Play delivery, every device/codec/provider or prolonged recovery performance. Remaining scenarios stay unpassed in the test report.

## Delivery documents and source review

The requested listing, privacy draft, Data safety draft, review access, signing, test report, release notes, baseline, separation and checklist files are in this folder. [Console instructions](PLAY_CONSOLE_INSTRUCTIONS.md) and [asset specifications](PLAY_STORE_ASSETS.md) cover owner submission preparation. [Manual maintenance](../docs/PLAY_MAINTENANCE.md) explains selective future ports without synchronization or customer changes.

A self-contained [privacy HTML draft](privacy-policy-draft.html) is ready for owner legal/contact completion; it has not been deployed. Ignored `artifacts/reviewer-demo/` contains exported synthetic TS samples, an M3U, a rolling XMLTV guide, rights/hosting notes and checksums. `scripts/export_review_demo.py` prepares an approved HTTPS base and regenerates the guide without publishing anything. The current loopback export is explicitly local-only.

The complete changed/added/removed source path inventory is [CHANGED_FILES.md](CHANGED_FILES.md). Local evidence under ignored `artifacts/` includes `final-artifact-verification.json`, `host-verification.json`, `device-validation-summary.json`, `private-trial-sanitized-summary.json`, `repository-independence.json`, `original-preservation.json`, `customer-release-artifacts-preserved.json`, core-parity/legacy-plaintext/test-separation reports and `SHA256SUMS.txt`. The final local commit/source ZIP record is produced after this documentation freeze. No completed-private-remote or byte-identical-original-Git claim is made.

Original-preservation qualification: `.git/FETCH_HEAD` and `.git/logs/refs/remotes/origin/HEAD` changed, and `.git/refs/tags/v1.0.9` appeared at the official `0612c0a` commit. No workspace agent issued a fetch/pull/tag or other mutating Git command against the original repository. The actor/cause is unconfirmed, so the metadata is preserved without restoration or deletion for owner review. Pre-restriction public comparisons found remote main/tag/latest release and release metadata unchanged; no GitHub mutation was performed. No further GitHub checks are permitted under the latest instruction.
