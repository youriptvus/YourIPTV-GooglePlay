> Current final delivery (November 18): GooglePlay-Release-Ready/START-HERE.md and Reports/Final-Delivery-Report.md are authoritative. Signed artifacts, current phone/TV/public HTTPS/ATF checks and public privacy verification passed; owner Console/signing/declarations/testing/approval gates remain. Earlier phase numbers and pending-hosting statements below are historical.

# YourIPTV completed privacy-policy text

Current completion: owner-supplied YourIPTV, admin@youriptv.us and https://youriptv.us are incorporated. The verified public /privacy URL currently has customer-only text; the exact completed replacement is [Privacy-Policy-Replacement.html](../GooglePlay-Release-Ready/Privacy/Privacy-Policy-Replacement.html). No deployment was performed. Use the [release package policy/verification](../GooglePlay-Release-Ready/Privacy/Privacy-Policy.md), not the older placeholder draft.

# YourIPTV Privacy Policy

Last updated: November 17, 2026.

Developer brand: YourIPTV. Application: YourIPTV. Website: https://youriptv.us. Support and privacy contact: admin@youriptv.us. Public policy address: https://youriptv.us/privacy.

## The player and its distributions

YourIPTV is an independent Android media player for sources you are authorized to use. It does not include channels, a provider account or a television subscription. A source account you configure is separate from a YourIPTV application account; the player does not create a YourIPTV cloud account or automatically synchronize your playlist database and viewing history between devices.

This policy covers the application website and the Google Play build as well as the separate customer APK distribution. Their update and connection behavior differs as described below. This policy does not announce Google Play availability.

## Information used on your device

The app stores playlist names and addresses, provider usernames and passwords where needed, guide addresses, configured user-agent settings, imported catalogues, guide data, artwork caches and provider-account information such as expiry and connection limits. It also stores favorites, viewing history and resume positions, playback and appearance preferences, local Admin/Settings/parental security state, restrictions, failed-attempt/lockout state, reminders and recording details. Recordings and backups you create remain in the destinations you choose.

These data support source access, playback, catalogue browsing, search, guide display, preferences, refresh, recording, restrictions and restore. The app does not upload its local playlist database or viewing history to a YourIPTV cloud-sync or analytics service.

## Connections and recipients

Your device requests provider details, playlists, guides, media and artwork from addresses you configure or that are supplied by those sources. Requests may contain source credentials, user-agent headers, content identifiers and the requested address. Some source protocols put credentials in URL paths or queries. Receiving servers also see ordinary connection information such as IP address and request time, and may associate requests with your source account.

The Google Play build requires HTTPS for network sources, retains platform certificate validation and blocks HTTP connections and HTTPS-to-HTTP redirects. Existing HTTP-only sources need working HTTPS addresses supplied by their operator. The separate customer APK distribution may support HTTP sources; HTTP does not encrypt transmitted credentials or content. HTTPS protects a connection in transit; it does not prevent the recipient from using information it receives.

Source operators and artwork hosts have their own privacy and retention practices. The player does not control every source a user can configure. Consult the relevant service about its records and practices. When you choose an external player, that application receives the stream address and title, which may contain credentials. The selected application handles them under its own policy.

## Updates, advertising and accounts

Google Play and Android deliver updates for the Play build; it has no direct APK self-installer. The separate customer APK distribution can check official release information and download an APK from its configured official update hosts, including the application website mirror or release hosting. Those hosts may receive normal request metadata. Their handling is separate from Google Play delivery.

The audited player includes no advertising SDK, analytics SDK, remote crash-reporting service or YourIPTV account-registration service. It does not sell personal information. Android, Google Play, selected providers, external players and document-storage services may process their own information under their policies.

## Local security, backup and exported files

The app uses Android app-private database/preferences storage. Provider credentials and credential-bearing addresses are not additionally encrypted by the app. Parental/custom Settings PINs and local Admin PINs, where used, are stored as salted hashes. Local PIN gates are configuration safeguards, not database encryption or a substitute for device security.

Automatic Android cloud backup is disabled. The Play build also declares explicit exclusions for system cloud backup and device-to-device transfer, including device-protected storage. Older customer builds and manufacturers' migration tools can behave differently. Compatible in-place updates retain app data.

An explicit JSON backup can include readable source credentials, addresses, favorites, supported preferences and restrictions. PIN hashes/salts, failed-attempt counters and the local Settings Lock flag are excluded from these exports. Protect backup files. Android's document picker can save to local, removable or cloud-backed storage selected by you; that service controls its copy. The app does not automatically upload a backup to a developer service.

## Retention and deletion

Local configuration remains until changed or removed with app controls, app storage is cleared or the app is uninstalled. Guide/catalogue caches are refreshed or pruned under app settings. Clear viewing history to remove the selected playlist's history/resume records. Delete a playlist to remove its source details and associated catalogue, guide, favorites, history, reminders and pending recording records. Finished recordings can remain and should be deleted separately from recording controls or their selected storage.

Clear app storage or uninstall to remove app-managed private data. These are destructive actions and are not required for routine updates. Exported files, external-player data, other people's copies and provider/hosting records are not removed by deleting a playlist or uninstalling. Delete exported copies separately and contact the relevant service about its records. There is no YourIPTV app account to delete.

## Website access and support messages

The application website provides product information, guides, release information and legal pages. Its hosting service can process access and security logs, including IP address, requested page/download, time and browser information, to deliver and protect the site. Hosting retention depends on the service configuration. These records are separate from the app's local playlist/history database.

If you email admin@youriptv.us, the mail service receives the information you choose to send, including your email address and message. It is used to answer support/privacy requests and investigate issues. Support and mail-security records are retained as needed for that purpose, applicable legal/security obligations and the mail-service configuration. Ask admin@youriptv.us about records, retention, correction or deletion. Do not email provider passwords, PINs, full credential-bearing playlist URLs or unredacted backups. Independently held provider records must be addressed to that provider.

## Controls, children and changes

Fresh Google Play preferences start with Settings Lock disabled; deliberate saved choices and ambiguous existing protection are preserved on compatible upgrades. Admin and parental restrictions remain independent. You choose sources, preferences, security and export destinations. Keep your device and accounts secure, especially on shared, rooted or unlocked devices.

YourIPTV is a general-purpose player, supplies no children's catalogue and does not verify the suitability of every external source. Parents and guardians should choose appropriate sources and manage device access. Local parental controls supplement those decisions.

This policy may change when application, website or data practices change. Its date identifies this edition. Send support questions, privacy enquiries and requests about information supplied to the maintainer to admin@youriptv.us.
