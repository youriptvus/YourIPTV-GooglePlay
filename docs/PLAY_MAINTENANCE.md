# Independent Google Play maintenance

Current maintained local candidate: **1.1.1 / code 52**, `com.youriptv.app`, minSdk 24, target/compile SDK 36. Customer and Play distributions keep independent repositories, build configuration and release artifacts. Shared feature behavior must be deliberately reviewed in both when requested; never overwrite distribution-specific signing, updater, permissions or initialization policy through blanket synchronization.

## Maintenance rules

1. Record the explicit local source baseline and owner-requested change. Work locally; GitHub pushes, remote release publication, Play submission and website deployment are separate actions. Owner publication remains manual for this preparation. A local URL or remote configuration is not evidence of remote privacy, current contents or a published canonical tag.
2. Preserve ordinary HTTP/HTTPS source compatibility for M3U, Xtream, XMLTV, artwork and media. Keep explicit schemes, platform TLS certificate/hostname verification and downgrade-redirect protection. Do not silently force HTTP sources to HTTPS or weaken TLS validation.
3. Preserve one app for phone/tablet/TV, Settings Lock OFF by default while retaining prior configured choices, independent Admin/parental restrictions, provider connection limits and source/account guards. Autoplay defaults ON only when unconfigured, preserves explicit OFF and stays accessible in normal Playback settings without an Admin PIN.
4. Keep Google Play-managed updates and automatic protection. Do not introduce a direct APK installer, installer permission, customer updater route or bundled provider credentials. Existing production signing remains compatible; never generate, replace or publish private keys as a maintenance shortcut.
5. Run the appropriate JVM/lint/artifact checks, shared-feature parity and meaningful isolated phone/TV regressions. Re-test manual watched/unwatched/clear precedence, stale-session rejection, stable source/content identity and captured checkpoint timestamps when modifying history/playback. New automatic retries or lifecycle callbacks must not mint replacement sessions after a manual history action.
6. Refresh release notes, current README/build instructions, privacy/Data safety drafts and artifact sidecars together after final verification. Preserve replaced delivery files and dated evidence. No new test count, hash, signed-source binding or canonical remote tag may be inferred from an older candidate.

## Version allocation and signing

Both distributions intentionally retain the same Android package and official signing identity. Coordinate versionCode allocation across installed customer builds and every Play track/upload, including drafts and retired uploads. **52 is the local candidate; its availability in the complete current Console history still requires owner verification before upload.** Do not reuse a consumed code or change it without rebuilding, retesting and refreshing all identities.

The expected public signing-certificate SHA-256 is `4aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539`. Public certificate identity is not private key material. Confirm the actual release artifact and current Play app-signing certificate/lineage, not merely the upload certificate. Keep `YOURIPTV_UPLOAD_SIGNING_PROPERTIES` and all key files external to source and archives.

## Submission gates

Recheck the current Console account/app state, actual closed-test eligibility, tester continuity, production-access controls and any review findings at publication time. Historical observations cannot establish today's counter. Keep public privacy/contact information accurate and available, disclose HTTP transport accurately, and review actual external-provider/export data flows. Preserve tester enrollment when updating the existing track. Test the actual Play-generated protected build on phone and TV before claiming delivered compatibility.

See [PLAY_BUILD_AND_TEST.md](PLAY_BUILD_AND_TEST.md), [PLAY_POLICY_AUDIT.md](PLAY_POLICY_AUDIT.md) and [PUBLICATION-REQUIREMENTS.md](../GooglePlay-v1.1.1/PUBLICATION-REQUIREMENTS.md). Prior contradictory HTTPS-only/code-48 guidance is retained solely as [dated history](history/2026-11-21-pre-1.1.1-refresh/PLAY_MAINTENANCE.md).
