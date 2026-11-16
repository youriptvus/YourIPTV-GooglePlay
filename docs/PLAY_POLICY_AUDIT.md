# Implemented candidate policy and security audit

Audit date November16,2026. Source and generated release merged manifest inspected from this independent project. This is a static/build-output snapshot, not Google approval or complete device QA. Recheck the final AAB and runtime before submission; report exact artifact in final handoff.

## Identity, SDK and form factors

Merged release manifest: com.youriptv.app, versionName1.0.9, proposedCode45, min23,target36. Compile36 in Gradle. One app module and one production release AAB; isolated .playtest debug configuration is QA only. Both MAIN/LAUNCHER and LEANBACK_LAUNCHER present. Leanback, touchscreen and landscape features optional; no required touchscreen excludes TV. Banner/icons declared. Main/child activity fixed-landscape manifest entries removed; RemoteActivity applies landscape for TV UI mode and system-bar/cutout/IME insets for shared screens. PlayerActivity resizeable/PiP. Static adaptation is implemented; current test report owns phone/tablet/TV runtime scope and remaining checks.

API36 meets current mobile requirement (Aug31,2026). Minimum23 meets TV requirement<=31. TV artwork dimensions/name, focus/Back behavior, memory/16KB/ABI, media keys and full-screen UI still need final artifact/device/Console validation.

## Permissions and component exposure

Generated merged release uses INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK and app signature permission com.youriptv.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. WAKE_LOCK is library-merged; signature permission protects AndroidX receiver compatibility. No REQUEST_INSTALL_PACKAGES, UPDATE_PACKAGES_WITHOUT_USER_ACTION, SYSTEM_ALERT_WINDOW, broad storage, location, contact, microphone, camera, notification, exact-alarm or boot permissions in inspected manifest.

MainActivity is exported solely for launch entry. Other app activities, including Settings/player/provider editor/Privacy, are nonexported. RefreshJobService is nonexported and requires BIND_JOB_SERVICE. AndroidX InitializationProvider is nonexported. AndroidX ProfileInstallReceiver is exported but requires system DUMP permission; INSTALL_PROFILE refers to runtime optimization profiles, not APK installation. Do not misclassify or remove that library receiver as an APK updater. No public app content provider or exported installer/update receiver exists in inspected manifest.

Package visibility uses narrow video VIEW schemes and document picker actions, not QUERY_ALL_PACKAGES. Source SettingsFlow screen-off receiver uses RECEIVER_NOT_EXPORTED. No PendingIntent path identified in app source, so no app-created mutable PendingIntent was found; dependency/runtime behavior is not thereby fully certified.

## Background work and lifecycle

Refresh uses JobScheduler/RefreshJobService with cancellable coroutines and application coordinator, not a foreground service or persisted boot receiver. No FGS type/permission/declaration required by current implementation. Preserve network/power scheduling limitations and overdue startup reconciliation; exact intervals cannot be guaranteed by Android. Recording/reminders remain existing app-running functionality; do not market stopped-app scheduling. Lifecycle/PiP/background video must be checked on devices; source presence is not an endurance/TV quality pass.

## Play update channel

Direct APK updater/UI/install classes and executable update configuration are removed by Play changes. No installer permissions/components in merged snapshot. Normal Google Play delivery needs no Play connection to watch configured sources and no phone-only in-app update dependency. TV support is not dependent on unsupported in-app update APIs. Final source/DEX/AAB audit required to rule out old hidden installer/endpoints; see test report. Customer updater remains unchanged elsewhere.

## Sources and transport

Generic Xtream/M3U/M3U Plus/XMLTV retained; customer preset shortcut removed from Play workflow. User credentials required only for sources that need them. Scheme-less provider input prefers HTTPS in Play changes; explicit HTTP remains for legitimate compatible streams. Network security config allows cleartext broadly because providers are user-selected, not a finite domain allow-list.

Http.open and Media3 retain cross-protocol redirects, including HTTPS-to-HTTP; initial HTTPS is not a guarantee all redirected transfers remain encrypted. Platform HTTPS certificate/hostname verification retained; no custom permissive TrustManager/HostnameVerifier or TLS bypass found. Do not mark all transmitted data encrypted-in-transit. This is a material documented transport limitation, not proof malicious providers are safe.

No bundled subscriptions or rights to third-party catalogues. Reviewer fixture/demo needs verified licensing and stable public access. Actual external services may log IP/content/account requests. Source logs use error/status metadata; StreamDiagnosis logs a host, so support logs can still reveal provider information. Do not upload raw logs/screenshots containing private sources/credentials.

## Credentials and local security

App-private SQLite stores provider passwords/credential URLs; database is not encrypted at rest. JSON backup/export intentionally includes credentials, with sensitive-file warning. Android automatic backup disabled. Imported or exported external documents remain subject to user-chosen provider/storage protection. No broad permission is required for scoped app storage/document picker.

Fresh empty settings prefs=>OFF. Preexisting settings prefs=>retain explicit lock or conservatively ON when absent. Initialize once, not per restart. Admin enrollment uses salted local PIN6-8 digits with lockout; proven legacy state accepts digest-verified old master until authenticated local enrollment. No plaintext universal secret in fresh Play code, and no unauthenticated reset bypass. These are local convenience gates, not database encryption or external identity assurance. Parental PIN/restrictions/lockouts remain independent; data-source/player limit behavior retained. Tests must verify both migration branches and On/Off/restarts.

## Signing and native code

OfficialSigning pins official public certificateSHA2564aef8798d4038bb8b20486b96b50af0870bca9d9b244a541a8973f76cecd0539 for production runtime. Play enrollment must use compatible certificate/lineage; an incompatible newly generated Play signing key would fail both Android update compatibility and the runtime gate. Upload key alone is not the installed APK signer. Debug QA bypass is intentionally restricted to nonproduction builds.

External signing properties and key paths enforced outside repository; no implicit customer-directory dependency. No key generated/replaced/enrolled. Final signature status is recorded separately. No native source/NDK declared, but transitive/final AAB .so scan still required. TV32/64-bit/16KB rules apply; Java/Kotlin-only artifacts satisfy architecture/page rules without bundling native variants, subject to runtime testing.

## Libraries and privacy

Pinned AGP9.5.0/Gradle9.8.1/Java17/API36 compatible per official AGP table. Media3 official stable1.11.1 matches source. Runtime declared AndroidX core1.17.0,appcompat1.7.1,recyclerview1.4.0,lifecycle2.10.0; coroutines1.10.2; Media3 ExoPlayer/HLS/UI1.11.1. No ads/analytics/cloud account/automatic crash SDK declared. Inspect resolved graph/final artifact; no claim every dependency is vulnerability-free.

Offline PrivacyActivity via public About renders bundled policy, no browser required on TV. Public website still owner prerequisite. Data safety draft maps providers/IP/credentials/images/export/transfers and does not infer no collection merely from no developer telemetry. Owner must confirm actual organizational practices/content-rating/ads/audience, publish policy and finish declarations.

## Remaining gates

Owner GitHub authentication/private remote; Console package/code45 history; compatible app-signing enrollment; public legal privacy/contact; stable legal reviewer sources; actual screenshot/artwork approval; prolonged playback/repeated TS interruption/runtime migration/tablet/accessibility; Play internal/closed testing and TV/device review. Unperformed checks stay unpassed. No submission/publication/approval claimed.

Official references:

- API: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en-en
- TV: https://developer.android.com/develop/adaptive-apps/quality-guidelines/tv-app-quality
- In-app update scope: https://developer.android.com/guide/playcore/in-app-updates
- Installer policy: https://support.google.com/googleplay/android-developer/answer/12085295?hl=en
- FGS: https://developer.android.com/develop/background-work/services/fgs/service-types
- Native/page sizes: https://developer.android.com/guide/practices/page-sizes
- AGP: https://developer.android.com/build/releases/agp-9-5-0-release-notes
- Media3: https://developer.android.com/jetpack/androidx/releases/media3
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
- Signing: https://support.google.com/googleplay/android-developer/answer/9842756?hl=en

See also GooglePlay-v1.0.9/PLAY_POLICY_AUDIT.md for policy research and dated distinctions.
