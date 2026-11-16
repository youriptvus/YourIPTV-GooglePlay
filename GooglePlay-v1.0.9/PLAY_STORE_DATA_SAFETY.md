# Data safety audit and declaration decisions

Source inventory, not a submitted form. Owner is responsible for all active artifacts, SDK behavior and organizational services. Verify final dependency graph, packaged code and actual runtime traffic; no traffic-capture pass is claimed here.

| Flow | Storage/recipient | Purpose/limits |
|---|---|---|
| Playlist names/URLs, usernames/passwords, guide URL/user agent | Db.kt/Repository.kt, app-private SQLite; selected servers | Source access. URLs may identify users/embed credentials. Private storage is not encryption. |
| Xtream API/media requests | XtreamUrls.kt; selected provider/stream/guide | Authentication/catalogue/guide/playback; credentials in URL path/query. HTTPS preferred; explicit HTTP remains. |
| IP and ordinary metadata | Provider/stream/guide/source image hosts via Http/Media3/image loader | Network recipients receive metadata; no claim they discard it. |
| Favorites, playback state, preferences, guide/cache | Local DB/preferences/files | Player/resume/guide/refresh; no developer telemetry endpoint identified. |
| Admin/Settings/parental security and lockouts | Local preferences | Local protection, no cloud authentication. Do not publish legacy secret/digest details. |
| Recordings | Local app folder | User-requested capture where permitted; no automatic cloud upload. |
| Backup/export JSON | Backup.kt/BackupFiles.kt; app folder or selected document destination | Includes credentials/preferences/favorites/restrictions. Chosen document providers can move it off device. |
| External playback/document actions | Chosen app/document provider | User initiation; recipient controls processing. |
| Update delivery | Google Play/Android | No custom APK installer/updater in final executable paths; verify artifact. |

## Libraries/permissions

Declared runtime libraries: AndroidX core/appcompat/recyclerview/lifecycle, Kotlin coroutines, Media3 ExoPlayer/HLS/UI. No Firebase, ads, analytics or automatic crash SDK declared. Review resolved transitives/final AAB before final declaration. INTERNET/ACCESS_NETWORK_STATE allow configured requests. Final merged manifest controls actual permission assessment, not this draft.

Automatic Android backup disabled. Broad storage/contact/location/microphone/camera/advertising-ID are not required player purposes. Update this record for new services/SDKs/permissions.

## Console decisions

Google defines collection as off-device transmission, including third-party libraries. No developer server is insufficient to conclude no collection. Assess credentials/user IDs/other personal info where applicable, request history and IP/device identifiers against actual data types/purposes. Distinguish collection from sharing exceptions, such as specific user-initiated transfers or service providers acting for developer. Do not blanket-exempt every provider transfer. Ephemeral processing can still require form disclosure.

Owner must confirm actual recipients, purposes, retention and developer-controlled services. Determine which transfer is required/optional and whether current form exemptions apply. No advertising/analytics purpose without evidence.

**Encryption in transit:** explicit HTTP is supported. Do not unconditionally answer all data encrypted in transit while HTTP credential/data paths remain. HTTPS uses platform certificate validation, but user-selected sources may not use it.

**Deletion:** local controls/Android storage actions remove applicable app data; exported files need separate deletion. No developer cloud account. Provider accounts/records belong to their services; do not claim developer can delete them. Answer account-deletion prompts according to actual account model.

Closed/open/production tracks require Data safety, including apps declaring no collection; internal-only testing has an exemption. Reconcile final form, public privacy and in-app notice. Source: https://support.google.com/googleplay/android-developer/answer/10787469?hl=en

Transport limitation: the preserved HTTP/Media3 paths permit provider redirects between HTTPS and HTTP. HTTPS entered initially is not a guarantee that every redirected transfer stays encrypted. No certificate-validation bypass was identified. The unconditional all-data-encrypted-in-transit declaration remains unsupported.
