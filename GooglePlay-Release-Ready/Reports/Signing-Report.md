> Current November19 candidate:1.0.9/code49,minSdk24/target36, automatic protection ON. See Reports/Protected-Candidate-Report.md and Protected-Candidate-Verification.json. Earlier code48 build/device counts below are historical; code49 has fresh host/artifact verification but no fresh protected Play-delivery pass. The owner authorized Console signing/test-draft operations.

# Final signing verification

Both final production artifacts passed independent signature verification against the existing official SHA-256 certificate:

`4aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539`

Package com.youriptv.app, version 1.0.9 / proposed code 48. Exact artifact hashes are in SHA256.txt and Artifact-Identity.json. Signing keys/properties remain external and excluded from Git. No signing-key creation/replacement or Play enrollment occurred.

The owner must confirm package ownership, unused code 48, and Play App Signing certificate/lineage compatibility in Console before upload. Matching local certificates alone does not establish the Play-generated installation/update path. The production APK was not installed over the customer app; final device tests use the separately signed .playtest QA package.
