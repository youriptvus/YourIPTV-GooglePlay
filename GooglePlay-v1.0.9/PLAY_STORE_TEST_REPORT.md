> Current final delivery (November 18): GooglePlay-Release-Ready/START-HERE.md and Reports/Final-Delivery-Report.md are authoritative. Signed artifacts, current phone/TV/public HTTPS/ATF checks and public privacy verification passed; owner Console/signing/declarations/testing/approval gates remain. Earlier phase numbers and pending-hosting statements below are historical.

# Play candidate test report

> **Current status, November 17:** [Active Play 1.0.9 / proposed code 48](CURRENT_STATUS.md) is owner-approved local preparation, pending unused Console-history verification. Code 45 artifacts are historical. The latest final-completion request authorizes safe isolated checks; customer packages/data remain protected. The owner attributes the separate playback issue to the server; no encoder/decoder changes are included.

The current HTTPS-only secure candidate, production source `cc13ebe5bba6dfba1fd00d38e976ce2c1756e4f3`, is being prepared separately under `artifacts/code48-secure/`. Secure-source host results passed 414 JVM tests / 57 classes, focused 61 / 6 classes, verifier tests 6/6 and full lint (0 errors, 272 warnings); final signing/compiled-artifact results are pending. The 401-test HTTP-integrity build and first 81-check code 48 artifact audit predate this transport change and are intermediate/superseded. Root-level code 45 evidence remains historical. See [current secure status](CURRENT_STATUS.md).

## HTTP-integrity phase: Play 1.0.9 / proposed code 48 (intermediate)

November 17, 2026. Production build source is `2c02256822cfc8c9c6b0436cceaeb1f4e7b3830a`. The raw encoded Content-Length guard rejects incomplete fixed-length responses, including gzip completion, before a shortened catalog can be accepted. Full host verification passed **401 tests / 56 classes**, with 0 failures, errors or skipped tests; full lint passed with **0 errors and 272 warnings**. Focused coverage was 35 tests / 4 classes, included in that full total. Post-fix synthetic localhost checks passed **16/16**, including complete 40,346,015-byte / 147,803-entry plain/gzip catalogs and fault responses.

These are PC/JVM and synthetic localhost checks. No new device/provider pass or TV EOF cause/fix is established. Clean early EOF without usable framing/Content-Length remains indistinguishable from legitimate shorter content. The new signed code 48 delivery is being prepared under `artifacts/code48-secure/`; packaging, signatures, artifact audit and final hashes remain pending. The root-level code 45 AAB/APK/source ZIP and its measurements below predate the guard and remain historical. See [current status](CURRENT_STATUS.md).

## Historical code 45 delivery and measured evidence

The following results are preserved measurements from the named prior runs. No current connectivity/installation guarantee or continuing device permission is implied; the later owner-requested TV uninstall does not retroactively change those results.

November 16, 2026 (America/New_York). Baseline: official `v1.0.9`, `0612c0acb500f096da5cfb766f6e7f0a012c4ba8`, code 44. Candidate: `com.youriptv.app`, `1.0.9`, proposed code 45 pending Console history. Production build commit: `f5a421823aa94fc8f652edc52050b8f12f5d7130`.

## Completed host verification

`testReleaseUnitTest lintRelease --console=plain --max-workers=1` completed successfully in 2 minutes 23 seconds. The retained log is `D:\APK\work\YourIPTV-GooglePlay-preparation\backup-test-lint.log`.

| Check | Actual result |
|---|---|
| Release unit tests | **385 tests / 55 classes; 0 failures, 0 errors, 0 skipped** |
| Full `lintRelease` | **Passed; 0 errors, 272 warnings** |
| Lint corrections | Media3 opt-in declarations, two narrowly documented dialog-authorization false positives and shared AndroidX Back handling; no blanket lint baseline |
| Retained warnings | Primarily internationalization, layout/style and available dependency updates; intentional cleartext transport is documented. A passing lint task does not certify every warning or dependency. |

Historical report-generation paths were `app/build/reports/tests/testReleaseUnitTest/index.html`, `app/build/test-results/testReleaseUnitTest/`, and `app/build/reports/lint-results-release.*`; those live paths now describe the latest build. Preserved code 45 host results are in [artifacts/final-host-checks.json](artifacts/final-host-checks.json). A clean build was performed before corrections; corrected sources were recompiled and verified by the final host and packaging commands.

`bundleRelease assembleRelease --offline --console=plain --max-workers=1` **passed in 2 minutes 12 seconds** using the existing external signing configuration. Log: `D:\APK\work\YourIPTV-GooglePlay-preparation\final-backup-package.log`. The isolated app/test build also passed in 15 seconds. Earlier pre-exclusion artifact hashes are intermediate and superseded.

The then-current post-device test-helper tree also passed `testReleaseUnitTest lintRelease` offline, retaining 385 tests and 0 lint errors. The final `final-tree-checks.log` completed in 8 seconds; the earlier `post-device-test-lint.log` completed in 26 seconds. At that code 45 historical freeze, production source matched its signed build commit.

## Verified production artifacts

Independent final verification passed **77 checks with 0 failed checks/errors**. Both artifacts have valid signatures from the official certificate `4aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539`.

| File under local `artifacts/` | Bytes | SHA-256 |
|---|---:|---|
| `YourIPTV-v1.0.9-GooglePlay.aab` | 5,508,849 | `26c0597e3616f37c779be477a31423ba4b5199348e981f2754c1b65c2d5c5d12` |
| `YourIPTV-v1.0.9-GooglePlay.apk` | 3,743,910 | `319737af5691bd7138814398e368d118e8df15c9b9e8a302b11d503ee24ce9d3` |

The AAB is the single production Play delivery. Verified metadata: `com.youriptv.app`, `1.0.9` / proposed code 45, min SDK 23, target SDK 36, nondebuggable, phone and TV launchers on the same activity, optional touchscreen/leanback/landscape features and bundled banner/icons. The merged permissions are INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK and the app's signature-only AndroidX receiver permission. No APK-installer/silent-update/overlay permission or executable installer path remains. No packaged native `.so`, plaintext legacy credential or test-only markers were found. Java/Kotlin-only packaging is architecture-neutral; a physical 16 KB device run is not claimed.

The actual AAB protobuf XML and APK binary XML were decoded: API 23 five-domain and API 24+ nine-domain full-backup variants, plus API 31+ cloud/D2D nine-domain exclusion sets, all match the intended boundary. The backup update left production DEX bytes identical to the preceding artifact. Report: ignored `artifacts/final-artifact-verification.json`. No private keys or credentials are part of these artifacts/source.

## Verification matrix

| Requirement | Current evidence and limit |
|---|---|
| Official public source identity | Latest release, tag and main verified at the exact baseline commit before import |
| Independent history | Initial root commit `1c633e5d6ac08203b76b3f13434c00b214c7123c`; own main/development branches and new-only origin |
| Original preservation | Tracked source, HEAD/index/config/local branches match the starting snapshot. Three fetch/tag metadata changes were observed; strict full-Git preservation did not pass and the cause is unconfirmed. |
| PRIVATE remote/push | Owner-manual only under the latest local-work instruction; no workspace GitHub request, creation, private verification or push |
| Code 45 unused in Console | Owner action; unavailable Console history is not proof of uniqueness |
| Fresh Settings Lock OFF | Host tests and actual isolated TV/phone Settings-without-PIN cases pass |
| Explicit ON/OFF and persistence | Host and TV/phone security cases pass. Both devices' real process restart/same-code replacement preserve OFF and active playlist ID. |
| Existing security migration | Synthetic legacy-policy inputs pass, preserving explicit choices and unknown protection; not a production customer upgrade |
| Admin authentication/lockouts | Host tests and isolated TV/phone Admin/lockout/dialog cases pass |
| Parental independence | Host PIN/restriction/lockout tests pass; no customer PIN modified |
| Provider connection limits | Known Xtream/account guards pass host tests and remain unchanged. Private M3U has effective app limit 0 (unknown); server reports max 1 and only one real stream was tested. No app-enforced 1 claim for arbitrary M3U. |
| Generic provider configuration | Host HTTPS/explicit HTTP/encoding/authorization and both devices' synthetic M3U/XMLTV import cases pass; private trial is separate |
| Playlist and EPG refresh | Host tests and actual TV/phone synthetic import/refresh/guide cases pass |
| MPEG-TS recovery/retries/buffer handling | Host tests and both devices' bounded repeated decoded/rendered clean-end observations pass with original backoff retained. Abrupt network/provider interruption unperformed. |
| Previous-channel/guide/navigation/lifecycle | TV D-pad/RIGHT/shared-player/Back and phone touch/rotation/fullscreen/RIGHT/Back checks pass across the qualified runs below |
| Direct APK installer removal | Host source and final merged AAB/APK/DEX audit pass; installer permissions and paths absent |
| Unified production identity/device declarations | Host and actual AAB/APK checks pass: one production package/version/player, both launchers and optional hardware |
| Signing configuration/credential boundaries | Host security/export tests and final official-signature/known-secret-pattern/legacy-plaintext separation audits pass |
| Offline public privacy entry | Host audit and actual TV/phone public About/privacy navigation pass |
| System backup/device-transfer boundaries | Host and actual compiled AAB/APK rules pass: 5 API 23 domains, 9 API 24+ domains and 9 domains in each API 31+ cloud/D2D section recursively excluded. No physical OEM transfer test claimed. |
| Production AAB and supplemental APK | Final packaging, 77-check artifact audit, signatures and hashes pass at the production build commit above |

## Real devices

| Target | Verified connection/installed customer baseline | Isolated run |
|---|---|---|
| onn. Streaming Device 4K Pro | `192.168.1.150:5555`, API 34; customer `1.0.9` / code 44 | Latest full run: 26/27 passed; remaining browser case separately passed 1/1 after harness correction. No single full 27/27 run is claimed. |
| Samsung SM-S166V | `R5GL75PGMFP`, API 36; customer `1.0.3` / code 34 | Full run: 28/30 passed; remaining two harness cases separately passed 2/2. No single full 30/30 run is claimed. |

The historical runs installed only temporary `com.youriptv.app.playtest` and its test APK. The same isolated APK ran on both targets. This describes those runs and does not authorize a new install or test. It is a debug test configuration, not another production application or AAB, and does not prove optimized release behavior or Play-generated APK compatibility.

Instrumentation uses synthetic local media/guide data, actual UI/key/touch events, shared-player frame observations and server-request counters. The latest TV full run passed 26 of 27 checks, including the 180-second soak under policy-derived recovery expectations: 5 stream ends, 4 new first-frame events, 8 full GETs, 0 player errors and a 32.548-second maximum observed gap. Existing later recovery/backoff delays remain; this is not an instant-reconnect or endurance guarantee.

The remaining browser assertion incorrectly required a navigation rail that the populated TV guide intentionally hides. Its corrected active-browser/visible-focus check passed separately 1/1, followed by successful real process restart and exact same-code APK replacement. OFF and active playlist ID persisted; customer installation metadata matched before/after. Evidence: `D:\APK\work\YourIPTV-GooglePlay-preparation\device-evidence\20261117T014031Z-192.168.1.150-5555`. Report the result as 26 checks from the full run plus 1 focused check, not a fabricated full 27/27 run.

Samsung full run `20261117T014144Z-R5GL75PGMFP` passed 28 of 30 checks, including actual touch Settings, Admin On/Off, rotation/fullscreen, RIGHT, Back, M3U/XMLTV and its 180.461-second soak (5 ends, 4 new first-frame events, 8 full GETs, 0 player errors, max observed gap 32.194 seconds). Two harness assertions needed correction: normal touch mode does not focus a browser view until D-pad input arrives, and a screenshot guard needed to wait for the intended window transition. The focused rerun passed 2/2, then real process restart/same-code replacement preserved OFF and active playlist ID with unchanged customer installation metadata. Evidence: `D:\APK\work\YourIPTV-GooglePlay-preparation\device-evidence\20261117T015046Z-R5GL75PGMFP`. This is 28 full-run checks plus 2 focused checks, not a claimed full 30/30 run.

Earlier failed evidence is retained: the first TV 26/27 run failed a soak threshold of at least 10 restarts/gaps below 30 seconds despite zero player errors (180.38 seconds, 5 ends, 4 new frames, 8 opens, max gap 32.55 seconds). A later harness attempt crashed on a main-thread UI assertion before completing. Test-only fixes made assertions safe, used bounded focus/window waits and aligned soak expectations with preserved baseline backoff. Production player/retry behavior and the verified AAB did not change. Both synthetic-device scopes are complete across the qualified runs; original full-run failure records remain preserved.

Actual process restart and same-code test-APK replacement are distinct from a versionCode-increasing customer/Play upgrade. Credentials, provider URLs, content titles, raw logs and private screenshots remain outside this repository/archive/public listing. Public reviewer demo files continue to use only generated synthetic media.

## Authorized private-source validation

The Samsung private trial passed 4/4 checks. Full M3U import produced 147,803 items (5,739 live, 32,965 movies and 109,099 episodes); XMLTV contained 53,030 programs. These are runtime test imports, not bundled content or a store-listing offer. Actual live playback used MPEG-TS (video/mp2t), H.264 (video/avc), 1280x720. During 60.141 seconds, the SDK counted 1,782 additional rendered decoder buffers; 120/120 samples were playing, with 0 player errors and 0 observed gap.

TV successfully imported a bounded three-entry private live subset through the parser in 269 ms, then connected DIRECTLY from TV to the provider for media. Candidate 2 passed 60.099 seconds with MPEG-TS/H.264 1280x720, 1,783 additional rendered decoder buffers, 120/120 playing samples, 0 errors and 0 gap. The small metadata subset came through a nonce-protected IPv4 loopback test endpoint; the media was not streamed through the host. This is not a successful full-catalog or full-private-EPG test on TV.

Retained limitations/failures:

- TV full 40,346,015-byte M3U download failed after 16.127 seconds with IOException caused by EOFException, classified truncated_response. Cleartext permission, foreground/interactivity and available storage were checked. The specific network cause remains unproven; this does not establish a VPN cause or a new-code regression. Full TV private EPG was not repeated.
- TV subset candidate 1 rendered and then stalled/reset: 59/120 playing samples, maximum observed gap 30.544 seconds. It did not pass. Candidate 2 subsequently passed; no all-streams stability claim is made.
- An earlier phone attempt imported the catalog/guide but the test selector incorrectly demanded a .ts URL suffix. Generic LIVE selection validated the actual MPEG-TS stream without a production-code change. Both tested provider output aliases returned valid M3U on the host; the alias itself was not proven invalid.

Each controlled trial released its player, deleted temporary inputs/playlist data, restored previous test preferences, preserved the customer installation and returned provider active connections to 0. The server reported maximum 1 and only one real provider stream ran at a time. The M3U app-effective limit was 0 (unknown); this is not proof of client-enforced 1 for arbitrary M3U, and known Xtream guards remain unchanged and host-tested. Decoder counters do not establish physical display/audio timing; sound is assumed.

Credential-free evidence: ignored artifacts/private-trial-sanitized-summary.json. It contains only approved aggregate metadata and retained failures. No private screenshots, source URLs, credentials, titles or raw logs are part of the repository, archive, listing or screenshot set.

## Unperformed or owner-dependent

No Play upload, Play App Signing enrollment, Play-generated split/certificate test, actual Play update, production-package upgrade/data migration, tablet-hardware test, long endurance, abrupt provider/network interruption, physical remote/display/audio timing, seven-day wall-clock EPG retention or 16-hour wall-clock refresh test is claimed. Any authorized private-trial result must be reported separately in sanitized form; it does not authorize bundling/submitting that account or content. Sound is assumed. Public reviewer hosting, privacy publication, Console declarations and required closed testing remain owner actions.

The preserved original Git metadata differences are `.git/FETCH_HEAD`, `.git/logs/refs/remotes/origin/HEAD`, and a newly present `.git/refs/tags/v1.0.9` at the official baseline commit. No workspace agent issued a fetch/pull/tag or other mutating Git command against the original; the actor/cause is unconfirmed. These files are preserved for owner review, not reverted or deleted. Pre-restriction remote comparisons matched; this workflow performed no GitHub mutation, and no further remote checks are permitted.

During the prior November 16-17 delivery, read-only customer-artifact checks also passed: the versioned 1.0.9 upload-folder APK, then-current generic APK and backup APK each were 3,749,548 bytes with the recorded official SHA-256 `6ac5ae11b0771ffd47c182a31318ff26ceaec53e7a3598110597af2c7a98b8fa`. Evidence: ignored `artifacts/customer-release-artifacts-preserved.json`. No customer release artifact was written or replaced.

Historical customer totals do not certify this candidate. Source/host assertions are not decoder or physical-device evidence, and connectivity is not functionality. No customer app/data clearing, uninstall, replacement, credential/PIN/launcher change is authorized to fill this report. No publication or Google approval is claimed.
