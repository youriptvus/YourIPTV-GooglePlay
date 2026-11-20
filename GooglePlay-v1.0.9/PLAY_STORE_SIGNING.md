# Signing and package compatibility

> **Current final signing handoff:** [Reports/Signing-Report.md](../GooglePlay-Release-Ready/Reports/Signing-Report.md). Existing keys remain external and unchanged. The final completion AAB signature/hash is recorded by the new build report; the prior identity below remains historical/reference.

> **Current status, November 17:** [Active Play 1.0.9 / proposed code 48](CURRENT_STATUS.md) is owner-approved local preparation, pending unused Console-history verification. Code 45 artifacts are historical. The latest final-completion request authorizes safe isolated checks; customer packages/data remain protected. The owner attributes the separate playback issue to the server; no encoder/decoder changes are included.

Production remains com.youriptv.app. Separate Git history does not change Android signing rules. Same package cannot ordinarily coexist with the customer app; updates need appropriate certificate compatibility and a higher versionCode.

## Current preparation

No key generation/replacement or Play enrollment is authorized. Keys/config/passwords stay outside the repository. `YOURIPTV_UPLOAD_SIGNING_PROPERTIES` optionally points to a protected external Java properties file; its keystore must also be outside the project. Template only in source. Without configuration release outputs are unsigned and not upload-ready. Never print passwords, put them in captured commands, or commit PEPK exports.

Existing official certificate SHA256 (public fingerprint):

`4aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539`

Actual final AAB signature status/certificate belongs in the test report. A locally official-signed bundle alone does not prove Play-generated APK compatibility.

## Owner enrollment decision

Evaluate enrolling the existing app-signing key through Console PEPK before rollout to preserve compatibility with official installed APKs. Compare official APK certificate with Console Play app-signing certificate. Do not accept an incompatible generated identity by default. Signing upgrades/lineages have Android-version-dependent behavior needing a deliberate plan.

An upload key authenticates bundle uploads; Play signs device APKs with its app-signing key. A separate upload key may be provisioned later by owner decision, not automatically here.

1. Confirm package ownership and all prior Console artifacts/codes.
2. Inspect official certificate fingerprints without exposing private keys.
3. Owner selects compatible enrollment/upload-key handling.
4. Follow the Console-provided PEPK instructions only after that decision; exports never go to GitHub.
5. Build using authorized external properties; verify AAB JAR signature/upload certificate.
6. Internal testing must verify generated APK certificate/lineage, versions, data migration and actual update path on an authorized safe device.

Code 45 identifies the historical artifact. Future compatible production codes must exceed known diagnostic 47 and be Console-unused; 48 is locally proposed, pending unused Console-history verification. The customer updater previousVersionCode field is irrelevant to Play delivery and never an intermediate-upgrade requirement.

## Manual versionCode coordination

The repositories and release processes are independent, but Android still sees one `com.youriptv.app` identity. The owner must manually coordinate versionCode allocation across both distributions and Play Console upload history. Keep a private allocation record; do not introduce automatic source synchronization or change the customer version from this project.

Customer code 44 is published and diagnostic code 47 has been used. Before any future release, select a Console-unused code greater than 47 and any later applicable customer/private code; the historical code 45 artifact cannot ordinarily update code 47. A Play code at or below an installed customer's code cannot provide an ordinary higher-version update, even with compatible signing. Select an unused higher code, then rebuild, retest, verify and re-hash. Likewise, a later manual move from Play to a customer APK needs a higher installed code and compatible signing; separate Git history does not bypass these Android rules.

Official: https://support.google.com/googleplay/android-developer/answer/9842756?hl=en and https://developer.android.com/studio/publish/app-signing
