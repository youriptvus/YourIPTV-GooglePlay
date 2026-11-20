# Next Play candidate: 1.1.0 / code51

The submitted 1.0.9/code50 closed-test bundle under `GooglePlay-Release-Ready` is preserved. This directory holds the next candidate independently; no Console upload or publication was performed.

Build with the external `YOURIPTV_UPLOAD_SIGNING_PROPERTIES` configuration. Prepared signed files are in the ignored `artifacts/` directory: `YourIPTV-1.1.0-code51.aab` and its companion APK. Keys and local build artifacts must not be committed.

See [release notes](../docs/RELEASE_NOTES.md), [validation](../docs/VOD_VALIDATION.md), [progress design](../docs/VOD_PROGRESS.md) and [reference observations](../docs/VOD_REFERENCE.md). The owner's local handoff contains hashes and detailed results.

The owner reviews and pushes the prepared feature branch manually. Before uploading the AAB, confirm code51 is still unused and higher than every Console upload, verify the app-signing identity, then create the desired test release manually. Keep the current testing release/participants intact until that deliberate update. Google review and production-access requirements remain Console decisions.
