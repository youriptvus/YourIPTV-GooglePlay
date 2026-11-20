> Current November19 candidate:1.0.9/code49,minSdk24/target36, automatic protection ON. See Reports/Protected-Candidate-Report.md and Protected-Candidate-Verification.json. Earlier code48 build/device counts below are historical; code49 has fresh host/artifact verification but no fresh protected Play-delivery pass. The owner authorized Console signing/test-draft operations.

# Final local production build

YourIPTV **1.0.9 / proposed code 48**, package **com.youriptv.app**. One production AAB supports phone/tablet/TV. Code 48 remains subject to Console-history verification.

The current signed release build passed in 5m13s after the accessibility corrections. **414 tests / 57 classes**, zero failures/errors/skips; **6 verifier regression tests** passed. Full release lint: **0 errors/fatal findings, 266 warnings**. The warnings remain visible in app/build/reports/lint-results-release.html. The first run retained an obsolete assertion requiring 40dp guide rows; it failed once, was updated for the 48dp minimum, and the complete suite passed. Logs are retained under D:\APK\work\YourIPTV-GooglePlay-completion-20261118T030147Z-a244466f\resume-20261118T223615Z.

The final isolated application/runner builds passed. Subsequent test-only fixes store evidence privately, wait for the newly launched app window to own input and tap only a target's visible bounds; no production code changed after this signed build. Compiled bundle/manifest/SDK/resource/transport/signatures and QA separation passed. No native .so library is packaged. Known trial secrets and legacy PIN literals were not found in executable/source materials. One numeric coincidence in R8 mapping line ranges was classified explicitly and the map retained unchanged; this is not a credential leak or a guarantee against unknown secrets.

Exact final artifacts: Artifact-Identity.json and SHA256.txt. Signature: Signing-Report.md. Original older release artifacts remain preserved in the recovery/history folders. This is local verification, not Google Play upload validation or approval.
