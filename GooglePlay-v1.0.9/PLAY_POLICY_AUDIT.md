# Google Play official-requirements research

> **Current status, November 17:** [Active Play 1.0.9 / proposed code 48](CURRENT_STATUS.md) is owner-approved local preparation, pending unused Console-history verification. Code 45 artifacts are historical. No `.150` testing/reinstall without a new explicit owner request. The owner attributes the separate playback issue to the server; no encoder/decoder changes are included.

Research date: 2026-11-16 (America/New_York). This is a policy research record, not an approval, source-code audit, device-test result, or final Data safety declaration.

## SDK and unified delivery

For new apps and updates, the current deadline is August 31, 2026: mobile apps target Android 16 / API 36 or higher. The TV-only exception is API 34. A unified mobile/TV application must meet the mobile API 36 requirement. Do not reduce target SDK. The November 1, 2026 extension date is already past.
Source: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en-en

Google explicitly supports the same package for mobile and Android TV and says adaptive apps can use one app bundle for multiple form factors. Opt in to Android TV distribution and pass its review; one mobile approval does not establish TV approval. A single AAB can produce optimized device APKs.
Source: https://developer.android.com/training/tv/publishing/distribute

## TV submission and runtime requirements

Verify MAIN + LEANBACK_LAUNCHER; make touchscreen and incompatible hardware optional; retain phone LAUNCHER. Use an in-app TV launcher banner 320x180 and an icon at least 160x160 at xhdpi, with the app name in the banner. Minimum SDK must be <=31. Every function needs five-way D-pad access and visible focus; Menu cannot be required. Back must ultimately reach TV Home. Landscape must fill TV without cut-off UI. Web content must use WebView, not launch a browser. Video should pause when the app is backgrounded. TV-G6 requires 32-bit and 64-bit plus 16KB page support from August 1, 2026. Supply reviewer access to the full app. These are runtime/quality criteria requiring actual validation, not just manifest entries.
Source (updated 2026-11-13): https://developer.android.com/develop/adaptive-apps/quality-guidelines/tv-app-quality
Implementation: https://developer.android.com/training/tv/get-started/create

## APK updating and Play delivery

REQUEST_INSTALL_PACKAGES may not be used for self-updates. An IPTV player has no documented installer core purpose; remove the permission and all installer/downloader/receiver/worker entry paths, not just buttons. Remove unused installer FileProvider paths and overlay/update components. Keep legitimate unrelated file import/export and PiP capabilities only if their own requirements remain satisfied.
Source: https://support.google.com/googleplay/android-developer/answer/12085295?hl=en

Play in-app update APIs support mobile phones, tablets and ChromeOS, not TV. Normal Play Store delivery satisfies the update channel without a new Play Core dependency. Optional future mobile in-app updates must be gated by supported form factor and never become playback prerequisites.
Source: https://developer.android.com/guide/playcore/in-app-updates

## Native code, 64-bit and 16KB

The canonical 16KB page-size guide currently states app-update enforcement February 1, 2027; older Google blog posts state November 1, 2025. Do not present that old general date as the current one. TV quality has its separate already-applicable August 1, 2026 requirement. Inspect final AAB/APKs for .so, including transitive dependencies. If all code including libraries is Java/Kotlin and no .so exists, architecture/page-size native rebuild requirements do not apply; still test on a 16KB environment when available. Native code needs compatible 64-bit alternatives, correct ELF LOAD alignment and APK ZIP alignment, and 32-bit support retained for older TV hardware.
Sources: https://developer.android.com/guide/practices/page-sizes
https://developer.android.com/google/play/requirements/64-bit

## Android 15/16 and background work

Declare a valid FGS type and type-specific permissions for any foreground service (Android 14+). Android 15+ prohibits mediaPlayback and dataSync FGS startup from BOOT_COMPLETED, and dataSync has timeout restrictions. Android 16 jobs, including WorkManager and DownloadManager, have updated quota behavior. Audit recording, reminders, EPG/playlist refresh and lifecycle paths; do not add unnecessary FGS to work that fits bounded background jobs.
Sources: https://developer.android.com/develop/background-work/services/fgs/service-types
https://developer.android.com/develop/background-work/services/fgs/changes

Target API36 applies edge-to-edge and large-screen orientation/resizability behavior changes. Exercise system bars, keyboard, multiwindow, configuration changes and portrait/landscape state; a manifest orientation lock alone does not prove tablet support.
Source: https://developer.android.com/about/versions/16/behavior-changes-16

## Privacy, Data safety and access

Closed/open/production tracks need Data safety; internal-only testing is exempt. Inventory all outgoing app/SDK data, not only developer-owned servers. Credentials/URLs go to selected providers; IP and request metadata go to content/EPG/image hosts. Determine whether each transfer is collected/shared, service-provider processing, or user-initiated transfer under the form definitions. Do not claim no collection solely because there is no analytics SDK. The secure candidate requires HTTPS and blocks insecure/downgrade requests; verify its final source, compiled policy and runtime scope before making the encryption-in-transit declaration. Check credentials, diagnostics, exports/backups and WebViews separately. Owner must confirm organizational practices and retention before submission.
Source: https://support.google.com/googleplay/android-developer/answer/10787469?hl=en

Privacy policy is required even if no personal data is collected. Publish a stable public non-geofenced non-editable HTML webpage (not PDF), identify YourIPTV/developer, and link it both in Play Console and in the app. Include contact, data types/purposes/recipients/security, retention/deletion and user choices. A local draft alone is not the public-policy prerequisite. Provide persistent reviewer instructions and any required authorized credentials in App access; do not place actual customer credentials in source.
Sources: https://support.google.com/googleplay/android-developer/answer/10144311?hl=en
https://support.google.com/googleplay/android-developer/answer/9859455?hl=en-en

## Signing and versions

Same package alone does not enable sideload-to-Play upgrades; APK signing certificates and Android versionCode must be compatible. Play App Signing can accept an existing app-signing key through the Console PEPK workflow; owner must choose this deliberately before rollout rather than accepting a newly generated incompatible key. A separate upload key only authenticates uploads; Play signs installed APKs with its app-signing key. Store keys outside Git. Inspect Console uploaded bundles/artifact explorer across all tracks to select an unused future code greater than known diagnostic 47 and any later applicable code; local absence is insufficient. Code 45 is the historical artifact identity; 48 is locally proposed, pending unused Console-history verification.
Source: https://support.google.com/googleplay/android-developer/answer/9842756?hl=en
https://developer.android.com/studio/publish/app-signing

## Account/testing/submission

Play Console registration currently has a US$25 one-time fee and identity verification. Choose accurate personal/organization identity, complete the account's requested verification, and create one app entry. Do not infer current package ownership, production access, country availability, or review approval from GitHub ownership.
Source: https://support.google.com/googleplay/android-developer/answer/6112435?rd=1

For personal accounts created after November 13, 2023: closed test with at least12 testers opted in continuously for14 days, then apply for production access with testing/readiness responses; eligibility does not automatically grant approval. Internal testing is optional and can precede complete listing setup. Open testing is available after production access. Confirm the actual account's Dashboard requirements.
Source: https://support.google.com/googleplay/android-developer/answer/14151465?hl=en

## Listing assets and copy

YourIPTV; Video Players & Editors is appropriate if the Console offers that category. Describe independent player, user-authorized Xtream/M3U/M3U Plus/XMLTV sources, mobile/tablet/TV, and no included channel service/subscription. No unverified performance/ranking claims. Name max30 characters and full description max4000 should be checked in Console; short description max80.

Required store icon512x512,32-bit PNG <=1024KB; feature graphic1024x500 JPEG/24-bit PNG without alpha; >=2 accurate screenshots total320..3840px with max dimension <=2x min. TV needs >=1 unaltered current TV screenshot and a separate store TV banner1280x720 JPEG/24-bit PNG no alpha. These differ from the in-app320x180 banner. Recommended promotional eligibility uses >=4 screenshots, minimum1080px,16:9 or9:16. Capture real running UI, legal demo media and redacted credentials; do not manufacture screenshots.
Source: https://support.google.com/googleplay/android-developer/answer/9866151?hl=en

For reviewer demonstration, Blender Foundation Big Buck Bunny is licensed CC BY3.0. Owner may host licensed sample content and an M3U/XMLTV demo under stable HTTPS with required attribution, without shipping a customer provider. A URL's availability alone is not proof of rights. Confirm live demo availability and review-access reliability before submitting.
License: https://peach.blender.org/about/
Downloads: https://peach.blender.org/download/

## Required owner actions

Confirm Console account/package ownership and all used versionCodes; enroll compatible app-signing certificate; provide legal developer/contact details; approve and publish privacy HTML; complete Data safety/content rating/ads/target audience based on audited behavior; provide stable authorized demo content; obtain actual phone/TV/tablet screenshots and prolonged device evidence; complete internal/closed tests and production-access process; validate device catalog and TV opt-in. No submission/publication is authorized by this research.

Transport limitation: the preserved HTTP/Media3 paths permit provider redirects between HTTPS and HTTP. HTTPS entered initially is not a guarantee that every redirected transfer stays encrypted. No certificate-validation bypass was identified. The unconditional all-data-encrypted-in-transit declaration remains unsupported.

## Audited toolchain/dependency compatibility

Source pins AGP9.5.0, wrapperGradle9.8.1, compile/target36,min23 and Java17. Official AGP9.5 compatibility supports maximumAPI37, minimumGradle9.6,JDK17 and BuildTools36, so this pairing meets the documented baseline. AGP includes Kotlin support; use the resolved build report for exact embedded compiler version rather than assuming standalone Kotlin latest. No native source/NDK is declared; final artifact scan remains required for transitives.

Media3 official page lists stable1.11.1 with latest page update November11,2026; project uses1.11.1 for shared ExoPlayer/HLS/UI. No device-specific engine is introduced. AndroidX/coroutines pinned source inventory is in Data safety; passing compilation alone is not a dependency vulnerability audit. No known-vulnerability-free certification is claimed.

Gradle official releases lists9.9.1(November10,2026) and9.8.2, newer than pinned9.8.1. Keeping compatible verified baseline avoids unnecessary release churn; assess patches before future maintenance. No Play policy requires newest patch solely by version number.

Sources: https://developer.android.com/build/releases/agp-9-5-0-release-notes
https://developer.android.com/jetpack/androidx/releases/media3
https://gradle.org/releases/
https://kotlinlang.org/docs/releases.html
