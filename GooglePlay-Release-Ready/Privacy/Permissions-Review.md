# Permissions and data-access review

Application: com.youriptv.app, current source1.0.9/proposed48. Final bundled permissions/signing are recorded in the final Build-Report and release audit; source manifest alone is not the final authority.

| Permission/component | Purpose and boundary |
|---|---|
| INTERNET | Fetch authorized HTTPS providers, playlists, XMLTV, media and source artwork. No runtime permission dialog. |
| ACCESS_NETWORK_STATE | Connectivity for source/refresh behavior. |
| WAKE_LOCK (library-merged) | Appropriate playback/background wake behavior; no location or microphone access. |
| App signature-only DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION | AndroidX receiver compatibility, not a data-collection permission. |
| Exported MainActivity | Phone/TV launcher entry. Other app activities remain nonexported. |
| RefreshJobService + BIND_JOB_SERVICE | Nonexported scheduled refresh with system binding protection. |
| AndroidX ProfileInstallReceiver + system DUMP | Runtime optimization profiles, not APK installation. |

The Play build has no REQUEST_INSTALL_PACKAGES, UPDATE_PACKAGES_WITHOUT_USER_ACTION or SYSTEM_ALERT_WINDOW. Direct updater/downloader/installer paths are removed. Narrow package queries find compatible external players/document handlers; they do not grant QUERY_ALL_PACKAGES or transmit an installed-app inventory.

No contacts, location, microphone, camera, advertising-ID, broad-storage, notification, exact-alarm or boot permission is required by the inspected source/previous compiled audit. Selected destinations use Android's document picker. No foreground service is declared; refresh uses JobScheduler and Android scheduling/network/power restrictions. Recording/reminders retain app-running limits.

Production cleartext is disabled and secure source/redirect guards are present. System cloud/transfer backup domains are excluded. App-private SQLite and explicit JSON exports are not additionally encrypted. Offline Privacy from About includes admin@youriptv.us and https://youriptv.us/privacy.
