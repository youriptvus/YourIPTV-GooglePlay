# Google Play Data safety answer sheet

Prepared from the Play source and current official form definitions. This is a concrete entry guide for the owner, not a submitted questionnaire or a claim that unknown provider practices have been verified. Apply it to every active Play artifact/region; customer APK distribution is separate. [Google form guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en).

## Main answers

| Console question | Proposed answer for this build | Basis / final owner check |
|---|---|---|
| Does the app collect or share required user data types? | **Yes.** Do not choose No collection merely because there is no developer analytics server. | Source credentials/account identifiers and requested media resources leave the device for selected servers. |
| Is app-controlled user-data transmission encrypted? | **Yes for the audited HTTPS-only Play transport.** | Production HTTP/downgrades are blocked; certificate validation remains. Reconfirm the final AAB audit and any additional organizational/SDK flow before attesting to the complete form. Manual exports and other apps have separate handling. |
| Does the app let users create accounts? | **No YourIPTV app-account creation.** | Adding an existing provider login does not create a YourIPTV account. No developer cloud registration endpoint exists. |
| Can users request deletion? | **Yes for information supplied to YourIPTV support through admin@youriptv.us; app-private data has local deletion controls.** | Policy explains deletion and external-provider limits. This does not promise deletion of records controlled by an independent provider. Confirm the mailbox/request process is operational. |
| Independent security review/accreditation | **No accreditation claimed.** | Local unit/lint/artifact tests are not a Google-recognized independent security certification. |
| Ads / advertising purpose | **No.** | No ad SDK, ad display or advertising-ID access identified. |
| Analytics purpose / automatic crash reporting | **No for the audited app.** | No telemetry/crash SDK or developer collection endpoint identified. Do not infer what an unrelated provider does with its records. |

## Data-type entries

Use the following supported profile. The sharing column does not presume every third-party transfer is exempt. A documented exception can change a form answer, but not the disclosed actual flow.

| Data type | Collected | Shared entry | Purpose / required choice |
|---|---|---|---|
| Personal info: User IDs | **Yes** for authenticated sources. | **Yes as the conservative profile** for provider/account recipients; use an exception only after documenting the exact user-initiated/disclosed flow. | App functionality; account management where authenticating the external account. Optional app-wide because anonymous sources are supported; required when the user chooses an authenticated source. Credentials can occur in URLs. |
| App activity: App interactions | **Yes** for requested catalogue/media identifiers and authenticated selection requests. | **Yes in the conservative profile** for selected media/artwork/guide recipients, unless a documented exception applies. | App functionality, not developer analytics. Requests are necessary for network playback, not separately disableable telemetry. |
| Device or other IDs / location derived from IP | **Owner-specific classification required; do not fabricate a blanket No.** | Match the actual recipient use. | Servers see connection IP/user-agent. The app does not read advertising ID, Android ID, IMEI or GPS. Google classifies IP by use: disclose location if used to infer location, or identifiers if used as identifiers. Confirm actual controlled-service/SDK practices. |
| Files/docs or exported user content | **Local data alone: No off-device collection. Explicit cloud/document export: assess that selected flow.** | User-requested export can meet a sharing exception; document it rather than exempting every transfer. | Optional restore/export functionality. Readable backup credentials are already in the User IDs flow. No automatic developer backup upload. |
| Email/name/messages sent to support | No app email field or embedded support uploader; voluntary email uses a selected mail app. | Assess any actual developer support integration added later. | Support correspondence is covered by the public policy. If a provider username is an email address, account for that identifier in the final form. |
| Precise location, contacts, health, financial/payment data, calendar, microphone/camera captures | **No corresponding collection in the audited app.** | No corresponding app transfer identified. | No relevant permission, billing flow or API use identified. Do not add unsupported categories merely because arbitrary user text could contain them. |
| Search history, favorites/history/PIN state | Local storage by default, not developer telemetry. | Only deliberate backup/external actions as described. | Local-only state is not off-device collection. A future remote-search/telemetry service would need reassessment. |

Do not mark provider processing ephemeral without evidence that the actual recipient retains it only in memory for the immediate request. The app cannot certify arbitrary server log retention. The supported conservative profile does **not** claim ephemeral provider processing. No advertising, marketing or analytics purpose should be selected without a real implementation/practice.

## Sharing and final verification

Google permits defined exceptions for service providers acting only for the developer, qualifying user-initiated/disclosed transfers and other specific cases. An independent source operator is not automatically the developer's service provider. Manual source selection/export differs from scheduled refresh and source-referenced artwork; review each flow instead of applying a universal exception.

Confirm only genuinely external facts before saving: whether the organization operates a configured source/hosting/support service, that service's IP/log/retention practices, any applicable sharing exception and all active Play variants. The separate IPTV service website was not inspected or assumed part of this app. Match the final answers with the public Play-inclusive policy. Live Console fields are authoritative; no CSV that could overwrite existing answers is supplied.

No YourIPTV registration system exists, so app-account-creation deletion duties are not triggered by such a system. Playlist deletion, clear history, storage removal and admin@youriptv.us requests are documented; external source-account deletion belongs to its operator. [User Data/deletion policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en).
