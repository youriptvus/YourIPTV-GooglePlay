# Current Google Play project status

Updated November 17, 2026 (America/New_York). This document supersedes current-state interpretations of the November 16 preparation and its early-November-17 test/release records. Their artifact identities, measured results and retained failures remain historical evidence. The existing AAB, APK, source ZIP and historical JSON records are preserved unchanged. [Machine-readable status](CURRENT_STATUS.json).

## Version and release status

The owner confirms that the customer **YourIPTV 1.0.9 / code 44 is published**. A separate, officially signed **1.1.0-diagnostic.1 / code 47** was subsequently used for private investigation. Diagnostic code 47 is a consumed local installation code; its use does not establish a Play Console upload.

The independent Play **1.0.9 / code 45** AAB/APK remains the recorded, signed historical candidate. Code 45 cannot provide an ordinary higher-version update over code 47, even with compatible signing. Any future production build intended for those installations must use an unused Play Console versionCode **greater than 47**, and also exceed any later applicable customer/private code. **48 is locally proposed, pending unused Console-history verification.** The owner must inspect all Console tracks, drafts and prior uploads and manually coordinate both distributions before final allocation. The owner has approved full local preparation of Play versionName **1.0.9**. VersionCode **48 is proposed for that preparation**, subject to Console history and any later applicable customer/private code; it is not verified unused in Console. The final HTTPS-only delivery is being prepared under `artifacts/code48-secure/`. The first code 48 build under `artifacts/code48/` is intermediate/superseded; root-level code 45 artifacts remain historical and unchanged. New build/test/signature results and hashes remain pending until recorded.

The owner now reports that the separate encoder/playback issue was **server-side** and has instructed this Play work to disregard that issue. This owner-confirmed disposition supersedes treating the earlier client decoder investigation as an active Play blocker; no fresh device/server test establishes the cause here, and no encoder/decoder change is included. The separate investigation's historical hold/diagnostic records remain outside this Play scope and are not altered. This does not identify or resolve the separately recorded TV full-catalog EOF or first subset-stream stall. Independent Play 1.0.9 preparation continues.

## Current HTTPS-only secure candidate (verification in progress)

The owner authorized full local compliance preparation. Current secure production build source: `cc13ebe5bba6dfba1fd00d38e976ce2c1756e4f3`. The current candidate requires HTTPS for provider, playlist, XMLTV, stream and artwork requests; rejects HTTP source/EPG input; disables production cleartext; blocks HTTPS-to-HTTP downgrade redirects; and retains platform certificate validation. Existing HTTP-only sources must be updated to working HTTPS addresses supplied by their provider. The in-app policy, public privacy/HTML drafts, Data safety draft and listing describe this compatibility change. Legal identity/contact placeholders and actual public privacy publication remain owner prerequisites.

The first signed code 48 bundle and its 81-check artifact audit predate this transport change and are **intermediate/superseded**. Final secure-source host verification passed **414 JVM tests / 57 classes** with 0 failures, errors or skipped tests; focused checks passed **61 tests / 6 classes**, standalone verifier tests passed **6/6**, and full lint passed with **0 errors / 272 warnings**. Signed release packaging passed in 3 minutes 19 seconds from the frozen secure source. The AAB (5,510,666 bytes) and APK (3,744,931 bytes) are under `artifacts/code48-secure/`; independent signature/compiled-policy/artifact verification and final hashes are pending. Eight repository-independence checks and actual secure-artifact legacy-plaintext/test-marker separation checks passed. The earlier 16/16 HTTP catalog fixture run is retained as pre-HTTPS evidence, not a new secure-candidate runtime pass. Final files will be prepared under `artifacts/code48-secure/`; no secure release-ready claim is made yet. A private GitHub repository is not a Google Play submission requirement and remains an optional owner-manual repository preference.

## Earlier code 48 HTTP-integrity verification (before HTTPS-only changes)

Intermediate HTTP-integrity build source: `2c02256822cfc8c9c6b0436cceaeb1f4e7b3830a`, Play 1.0.9 / proposed code 48. Final secure production source is `cc13ebe5bba6dfba1fd00d38e976ce2c1756e4f3`; its separate results are recorded above. A PC/JDK probe reproduced a declared HTTP Content-Length of 4,158 bytes with only 62 bytes delivered: because the shortened body ended with a complete M3U entry, the previous path accepted one entry. The new guard counts raw encoded bytes before decompression and rejects a premature EOF for a usable fixed Content-Length, including completion of decoded gzip input. Intentional early close, unknown-length/Transfer-Encoding handling, parser/publication behavior, retries, timeouts, provider limits and the player remain unchanged.

| Current local check | Verified result |
|---|---|
| Focused JVM tests | 35 tests / 4 classes; 0 failures, errors or skipped |
| Full release JVM tests | 401 tests / 56 classes; 0 failures, errors or skipped |
| Full lintRelease | 0 errors, 272 warnings |
| Post-fix synthetic localhost checks | 16/16 passed, including complete 40,346,015-byte / 147,803-entry plain catalogs, gzip (1,343,718 encoded bytes) and fault cases |
| Intermediate code 48 bundle/APK | First signed build passed 81 artifact checks, then was superseded by HTTPS-only compliance changes; not the final secure delivery |
| Fresh device/provider validation | Not performed |

Evidence: `D:\APK\work\YourIPTV-GooglePlay-catalog-eof-20261117\final-validation-summary.json`. The focused tests are included in the full total. These are PC/JVM and synthetic localhost results, not Android/device/provider/database-runtime observations. A clean early end without usable framing/Content-Length cannot be distinguished from a legitimate shorter playlist.

This does not fix or establish the cause of the historical TV full-catalog EOF, and no new TV query/playback/reinstall is performed. The existing code 45 AAB/APK and source ZIP predate the guard and cannot validate it. Final secure release preparation uses `artifacts/code48-secure/`; earlier `artifacts/code48/` output is superseded and preserved.

## Device state and authorization

The later owner-requested removal supersedes the earlier TV installation snapshots. The uninstall evidence timestamp is `2026-11-17T22:45:29.8407838Z`. On `.150`, both exact packages `com.youriptv.app` and `com.youriptv.app.playtest` were uninstalled, without keeping their app data, and an independent package query recorded both absent. This is a recorded result from the separate investigation; no new device query was made for this status update.

**Do not resume player testing or reinstall either package on `.150` without a new explicit owner request.** Continuing local Play preparation does not grant device-test or reinstall permission. Earlier TV/phone connection, customer-preservation, restart, replacement and playback results describe their named historical runs; they are not current installation/connectivity guarantees. The current Samsung state is not newly verified.

## Permitted continuation and preserved evidence

The resumed work includes local documentation/status work, source review and synthetic JVM/localhost catalog diagnostics. Prior authorization for full local development, testing and builds remains in place, and the owner has now approved full local Play 1.0.9 preparation while preserving the original customer repository. Code 48 is the proposed next Play code; Console history remains unverified. This documentation update itself does not build or replace artifacts. The current source contains the integrity guard and local versionCode 48; versionName remains 1.0.9. No GitHub calls, remote verification, pushes, uploads, Play submission, signing-key creation/replacement/enrollment, device/provider sessions or automatic customer synchronization were performed during this continuation. Standing GitHub, signing, submission and device restrictions remain. The owner publishes manually.

The historical `artifacts/LOCAL_RELEASE_RECORD.json`, artifact audits, device/private-trial summaries, `CHANGED_FILES.md` freeze inventory, customer-preservation checks and source ZIP describe the preceding November 16-17 delivery. They are not fresh preservation or installation checks performed for this status correction. Do not rewrite them to suggest a new client decoder fix was implemented, the TV remains installed, or previously failed cases passed. Record any new code 48 evidence separately. Later source documentation is outside that historical ZIP. The existing full-TV private catalog EOF/truncation and first subset-stream stall remain unresolved in the historical report.

Local sources for the superseding facts are the owner instructions dated November 17 and `D:\APK\work\YourIPTV-series-investigation-20261117T041158Z-e075dbe6\CURRENT_STATE.md`, especially its top owner-requested removal section. No network, GitHub, private-configuration or fresh device inspection was required.
