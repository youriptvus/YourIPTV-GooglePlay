# Protected Google Play candidate - November 19

Current release is **YourIPTV 1.0.9 / code 49**, package **com.youriptv.app**, **minSdk24 (Android 7.0+) / target36**. The Android6-only fallback was removed by Android's optimizer because that version is no longer supported by this protected candidate. Credential and device-transfer exclusions remain verified on supported Android versions. No production app security control was removed.

**Automatic protection remains ON.** The earlier code 48/min23 upload failed Google's minimumSDK 24 protection validation. Its draft was discarded; new internal draft2 was created after protection was restored, and its enhancements explicitly show protection ON. Console status: **available_to_internal_testers_not_reviewed**. Existing app-signing and upload certificates both match **4aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539**, verified after the owner-authorized encrypted PEPK transfer. No production key was created or replaced locally.

Fresh verification: signed bundle/APK, bundletool validation, compiled manifest/network/backup checks, **414 tests / 57 classes**, **8 verifier tests**, full lint **0 errors / 265 warnings**, production/QA separation and known private-token scan passed. Detailed retained evidence: `D:\APK\work\YourIPTV-GooglePlay-protected-20261119T190746Z-b38a8ab1`. The APK is a local verification artifact; upload the AAB for Play distribution.

New code 49 physical-device and protected Play-delivery tests have **not** passed yet. November 18's code 48 Samsung35/35, TV32/32 and public HTTPS/ATF14/14 per device remain historical evidence. An API 36 / 16 KB emulator booted but did not complete a new app test. It has no Google Play Store; installer verification was not spoofed or bypassed.

All **1124** customer repository files, including Git metadata, match their pre-run hashes. No customer installation/data was modified. No GitHub action, website deployment or production publication occurred. Only the explicitly authorized Google Console signing/setup/test-draft operations were performed.

Remaining publication gates: finish account-specific app-content declarations and store listing, test actual Play-generated protected delivery, enable/review Android TV for this same app/AAB, complete the Dashboard's 12-testers / 14-days closed test, obtain production access and Google approval. Local checks and accepted upload do not certify approval or uninterrupted provider playback.

Developer YourIPTV; support/privacy admin@youriptv.us; public policy https://youriptv.us/privacy. Keep public reviewer media and rolling EPG available through review. Original code 48/source artifacts and prior reports are retained in the recovery directory and dated backup archive. Source is uncommitted for owner review.

The owner-authorized internal release is active and available to the two confirmed testers. Play temporarily labels it `com.youriptv.app (unreviewed)` until app setup and review are complete. Internal testing does not start or satisfy the required closed-testing period. No production release was published.
