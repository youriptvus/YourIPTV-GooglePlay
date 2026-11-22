# YourIPTV Google Play local handoff

Updated November 21, 2026. Current maintained candidate is **1.1.1 / code 52**, package `com.youriptv.app`, minSdk 24, target/compile SDK 36. It remains an unpublished local amendment; this handoff does not claim an upload, current Console counter or production approval.

The release includes grouped parent-series history, per-episode resume and green progress, guarded manual watched/unwatched/reset actions, finite cross-season autoplay and manual Next, phone/TV navigation, catch-up archive routing and source checks, remembered live startup, held Back fullscreen return and guide opening at the current programme. Existing saved history/security choices are retained. Settings Lock is OFF by default; autoplay is ON only when unconfigured and saved OFF is preserved. Normal Playback settings expose autoplay without an Admin PIN.

Explicit HTTP/HTTPS sources work without silently forcing HTTPS. Platform HTTPS validation and downgrade-redirect blocking remain. Play signing, automatic protection and Play-managed updates remain unchanged; no direct APK installer is included.

## Verification and artifacts

Final isolated-package device evidence reports **185 passing checks**: Moto API 36 (93), onn. TV API 34 (92), with production application identities preserved. Evidence and per-suite provenance: `D:\APK\work\YourIPTV-history-device-qa-20261121T163606Z\watch-guard-final\final-summary.json`. The isolated app APK hash is `02afe9ef2c7956258bf7501d53b7da6fc87dc14515ef61c1344ddf7dfcc13fc0`; this is not the production AAB hash. Samsung debugging authorization was unavailable; tests used generated decoded media rather than a private provider/reference-series catalog. Sound was assumed. Superseded failures remain in the evidence record.

Use the refreshed 1.1.1 artifact sidecars for final source commit, release APK/AAB hashes, JVM/lint totals, signing and package verification. No final build statistic or release hash is inferred here. Preserve prior deliveries before refreshing current files; do not upload isolated QA APKs as production artifacts.

## Publication remains an owner action

Verify code 52 is unused in all current Console uploads/tracks, confirm the current app-signing certificate/lineage, keep automatic protection enabled and use the existing app/testing track. Expected public certificate SHA-256 is `4aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539`. Private signing material stays external and is never an upload attachment.

Update/verify public privacy and Data safety for actual HTTP/provider/export handling, current listing/reviewer access and any Console-required declarations. Recheck actual tester continuity, elapsed closed-test time, production-access controls and review findings; keep enrolled testers in place. Test the actual Play-generated protected code-52 build on phone and TV. Local QA does not replace this delivery check or Google's decisions. Verify any remote canonical tag/source provenance manually; no GitHub push or remote state verification is asserted here.

## Historical Console observations

The archived November 19 handoff records submission of code 50 and declarations. A November 20 observation recorded a published closed test, Android TV Active, at least 12 opted-in testers and production eligibility still disabled while the continuous test period was incomplete. Those are dated observations, not a current status check. Any December eligibility estimate was conditional on continuity and Google's counter, not a promised unlock date. Earlier zero-tester/recruitment-pause text is also historical.

Exact previous wording is preserved in [docs/history/2026-11-21-pre-1.1.1-refresh/HANDOFF.md](docs/history/2026-11-21-pre-1.1.1-refresh/HANDOFF.md). Current working instructions: [build/test](docs/PLAY_BUILD_AND_TEST.md), [maintenance](docs/PLAY_MAINTENANCE.md), [policy record](docs/PLAY_POLICY_AUDIT.md) and [PUBLICATION-REQUIREMENTS.md](GooglePlay-v1.1.1/PUBLICATION-REQUIREMENTS.md).
