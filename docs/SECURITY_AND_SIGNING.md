# Security and signing

Signing properties, private keys, passwords, tokens, provider/customer credentials, device identifiers and confidential test records stay outside public source, exports and archives. The source exporter reads pinned Git objects through a controlled allowlist and validates the entire resulting public tree before writes. A missing private restricted-term list fails closed. Historic confidential reports remain on disk and in existing history but are removed from the current public source tree; history is not rewritten.

Both source selections preserve package com.youriptv.app. Customer production requires minSdk23; protected Play delivery requires minSdk24, both targeting36. Existing upload and customer signing configurations are independent inputs, never debug fallbacks. The release tool checks each actual artifact against its separately pinned public certificate. No key is created, replaced or automatically uploaded.

The existing customer 1.1.1 APK and Play upload AAB were inspected read-only. Their public signer SHA-256 is:

```text
4aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539
```

Read-only Play Console inspection on November22,2026 confirmed that its current app-signing certificate has the same fingerprint. This separately verifies the Play-delivered signing identity; matching an AAB upload certificate alone would not establish that compatibility.

The common package/certificate permits a future compatible Play update over an official customer installation, provided the Play versionCode is higher, the device meets API24 and other delivery requirements, and Google Play offers the app to that account/device. Equal code52 is not a new upgrade. API23 customer devices cannot receive this Play build. Availability, distribution/protection checks and Play eligibility still apply; this is not a guarantee for every device. No installation/uninstall or customer data modification was performed for this inspection.

User-selected HTTP and HTTPS sources are accepted. HTTP sends source credentials/content without TLS. HTTPS uses platform certificate/hostname validation; downgrade redirects are blocked. No permissive verifier or protection disabling is needed. Customer APK update transport remains its existing validated HTTPS contract; Play delivery contains no customer updater implementation/installation permission.

Settings Lock defaults off unless an existing explicit setting is saved. Parental controls remain independent. The customer master policy and Play per-install Admin policy are intentionally selected separately to preserve current installations and upgrade provenance.

Automated guards supplement review. They detect forbidden/private files, high-confidence credentials, unsafe paths and privately supplied restricted terms; they cannot prove that every possible unknown credential or text within raster images is absent. Public screenshots/assets and external listings require separate review. Do not alter old commits or delete historic releases merely to remove old wording; document historical findings privately.

Official references: [Play App Signing](https://developer.android.com/studio/publish/app-signing), [Android app updates](https://developer.android.com/studio/publish/versioning), [GitHub Actions security](https://docs.github.com/en/actions/reference/security/secure-use), [scoped installation token action](https://github.com/actions/create-github-app-token).
