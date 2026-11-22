# Current Google Play candidate policy and security record

Updated November 21, 2026 for local **1.1.1 / code 52**. This records implemented behavior and measured scope; it is not Google approval or a guarantee of future policy acceptance. Historical candidate audits are preserved in [the dated archive](history/2026-11-21-pre-1.1.1-refresh/PLAY_POLICY_AUDIT.md).

## Application and delivery

Production package `com.youriptv.app` uses one shared phone/tablet/Android TV implementation and AAB, minSdk 24 and target/compile SDK 36. The `.playtest` package is isolated QA only. Existing Google Play automatic protection, signing compatibility and Play-managed updates remain unchanged. The Play project has no direct APK self-installer/customer updater. Verify the final compiled manifest, DEX and AAB with the artifact audit and refreshed sidecars; historical artifacts do not establish the current candidate's exact permissions or hash.

Physical held Back uses the documented Android 16 navigation compatibility setting `enableOnBackInvokedCallback=false` so the app receives down/up duration. Normal Back uses its existing AndroidX handlers; predictive Back system animations are disabled. This is navigation routing, not a change to Play protection, signing, TLS or permissions. See [Android's target-36 behavior documentation](https://developer.android.com/about/versions/16/behavior-changes-16#predictive-back).

## Sources, privacy and local data

User-entered HTTP and HTTPS playlist, Xtream, XMLTV, artwork and playback URLs are accepted, preserving the explicit scheme. HTTP traffic is not encrypted by TLS. HTTPS retains platform certificate/hostname validation; HTTPS-to-HTTP redirects remain blocked. No permissive TLS verifier, hidden HTTPS fallback or provider preset is introduced. The earlier HTTPS-only rule describes previous candidates and must not be used for current setup or privacy claims.

External providers can receive IP addresses, account credentials and content requests; user-selected exports can contain credentials. Absence of a developer telemetry SDK does not establish absence of collection by external services. Public privacy policy and Data safety answers must match these actual flows and HTTP handling before publication. Current organizational/host logging practices and final Console answers remain owner-verification items. No all-traffic-encrypted or automatic-deletion claim is made.

Provider credentials remain in app-private local storage; this is not database encryption. Manual backup/export intentionally carries sensitive provider configuration and must remain user-controlled. Retain disabled Android automatic backup and scoped document access; do not introduce broad storage access or copy private configurations into source, reports or release archives.

## Access and playback state

Settings Lock defaults OFF unless an existing choice/security state must be retained. Admin enrollment and parental controls remain independent; autoplay is a normal Playback setting available without an Admin PIN. Autoplay is ON only for unconfigured preferences, preserves saved OFF, follows finite same-series/source episode queues and does not wrap. Source/account guards apply to archived and current playback; no catch-up item is converted into an ordinary catalog movie or written as watch progress.

Watch history preserves existing records without schema migration. Manual watched/unwatched, item reset and playlist-history clear invalidate earlier process-local playback sessions. Passive checkpoints and completion writes carry their original source/content session and observation time. Stable content identity, rather than a title, row ID or raw rotating URL, determines session reuse. New intentional replay may create a fresh session; automatic recovery/lifecycle cannot bypass manual invalidation.

## Signing and release boundaries

Expected public certificate SHA-256: `4aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539`. Verify actual final APK/AAB signatures and current Console app-signing certificate/lineage. Upload signing and installed-app signing are distinct roles. Signing configuration/key files stay external; no new key or enrollment change is part of this amendment. Source, release and backup archives must exclude private keys, credentials, databases and sensitive diagnostics.

## Evidence and remaining gates

The final isolated-device record reports **185/185 checks passed**: Moto g (2025), API 36, 93; onn. Streaming Device 4K Pro, API 34, 92. Production identities remained unchanged. Canonical local evidence is `private local evidence`; the suite-specific test APK and unchanged isolated app identities are recorded there. Generated video was decoded; Samsung authorization, private-provider/reference-series testing, physical tablet coverage and sound confirmation were unavailable/not performed. Sound was assumed. This evidence does not certify code-52 protected Play delivery.

Use refreshed final build/test/artifact sidecars for JVM totals, lint results, source binding and release hashes. Recheck current target/device policy, unused code 52 across all uploads, public privacy/contact and Data safety, lawful reviewer access/assets, current closed-test/production eligibility and Google review at submission. The live timer/counter, remote canonical tag and final code-52 Play-delivered behavior have not been reverified by this local documentation update. See [PUBLICATION-REQUIREMENTS.md](../GooglePlay-v1.1.1/PUBLICATION-REQUIREMENTS.md) and [development workflow](DEVELOPMENT_WORKFLOW.md).
