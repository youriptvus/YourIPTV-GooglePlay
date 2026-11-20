# Data safety audit and declaration decisions

> **Current answer sheet:** [Privacy/Data-Safety-Answers.md](../GooglePlay-Release-Ready/Privacy/Data-Safety-Answers.md) gives concrete Console answers and the genuinely recipient-specific checks. Brand/contact are now supplied; no blanket no-collection or universal sharing exemption is asserted. This older source inventory remains supporting evidence.

Source inventory, not a submitted form. Owner is responsible for all active artifacts, SDK behavior and organizational services. Verify final dependency graph, packaged code and actual runtime traffic; no traffic-capture pass is claimed here.

| Flow | Storage/recipient | Purpose/limits |
|---|---|---|
| Playlist names/URLs, usernames/passwords, guide URL/user agent | Db.kt/Repository.kt, app-private SQLite; selected servers | Source access. URLs may identify users/embed credentials. Private storage is not encryption. |
| Xtream API/media requests | XtreamUrls.kt; selected provider/stream/guide | Authentication/catalogue/guide/playback; credentials in URL path/query. HTTPS required; insecure HTTP and HTTPS-to-HTTP redirects blocked in the current secure candidate. |
| IP and ordinary metadata | Provider/stream/guide/source image hosts via Http/Media3/image loader | Network recipients receive metadata; no claim they discard it. |
| Favorites, playback state, preferences, guide/cache | Local DB/preferences/files | Player/resume/guide/refresh; no developer telemetry endpoint identified. |
| Admin/Settings/parental security and lockouts | Local preferences | Local protection, no cloud authentication. Do not publish legacy secret/digest details. |
| Recordings | Local app folder | User-requested capture where permitted; no automatic cloud upload. |
| Backup/export JSON | Backup.kt/BackupFiles.kt; app folder or selected document destination | Includes credentials/preferences/favorites/restrictions. Chosen document providers can move it off device. |
| External playback/document actions | Chosen app/document provider | User initiation; recipient controls processing. |
| Update delivery | Google Play/Android | No custom APK installer/updater in final executable paths; verify artifact. |

## Libraries/permissions

Declared runtime libraries: AndroidX core/appcompat/recyclerview/lifecycle, Kotlin coroutines, Media3 ExoPlayer/HLS/UI. No Firebase, ads, analytics or automatic crash SDK declared. Review resolved transitives/final AAB before final declaration. INTERNET/ACCESS_NETWORK_STATE allow configured requests. Final merged manifest controls actual permission assessment, not this draft.

Automatic cloud backup remains disabled. Explicit legacy backup rules and Android 12+cloud/device-transfer rules exclude all credential-protected and device-protected domains, preventing the framework's default D2D inclusion. Compatible in-place updates and explicit JSON export are separate and unchanged. These declarations have parsed-resource coverage; no physical OEM transfer test is claimed. Broad storage/contact/location/microphone/camera/advertising-ID are not required player purposes. Update this record for new services/SDKs/permissions.

## Console decisions

Google defines collection as off-device transmission, including third-party libraries. No developer server is insufficient to conclude no collection. Assess credentials/user IDs/other personal info where applicable, request history and IP/device identifiers against actual data types/purposes. Distinguish collection from sharing exceptions, such as specific user-initiated transfers or service providers acting for developer. Do not blanket-exempt every provider transfer. Ephemeral processing can still require form disclosure.

Owner must confirm actual recipients, purposes, retention and developer-controlled services. Determine which transfer is required/optional and whether current form exemptions apply. No advertising/analytics purpose without evidence.

**Encryption in transit:** the current secure candidate requires HTTPS for provider, playlist, XMLTV, media and artwork requests, blocks insecure HTTP and downgrade redirects, and retains platform certificate validation. Final compiled-artifact verification is pending. Confirm the complete app/SDK/organizational transfer inventory before answering the Console form; recipient handling and deliberately selected external apps/document providers remain distinct from the app's own HTTPS transport.

**Deletion:** local controls/Android storage actions remove applicable app data; exported files need separate deletion. No developer cloud account. Provider accounts/records belong to their services; do not claim developer can delete them. Answer account-deletion prompts according to actual account model.

Closed/open/production tracks require Data safety, including apps declaring no collection; internal-only testing has an exemption. Reconcile final form, public privacy and in-app notice. Source: https://support.google.com/googleplay/android-developer/answer/10787469?hl=en

Compatibility change: existing HTTP-only provider/playlist/guide/media/artwork addresses no longer connect in the secure Play candidate. Obtain working HTTPS addresses from the source owner and update saved configuration; imported data is not a grant to make an insecure request. Historical code 45 HTTP trials and the first code 48 bundle predate this policy and do not validate the secure candidate.
