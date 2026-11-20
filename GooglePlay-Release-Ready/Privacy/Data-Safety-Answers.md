# Google Play Data safety answer sheet

The form described below was finalized and included in the17 changes sent to Google on November19,2026 at21:59UTC. Status: **in review**, not Google-approved. Apply it to every active Play artifact/region; customer APK distribution is separate. [Google form guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en).

## Submitted form and audited scope

Collect/shareYes; encrypted app-controlled transportYes; no YourIPTV account creation; external source accountsYes, created outside the app by the source operator. User IDs:collected/shared,optional,not ephemeral,App functionality. App interactions:collected/shared,required for network functionality,not ephemeral,App functionality. Files/docs:collected,optional,not ephemeral,App functionality,not shared under the specific user-initiated destination-selection exception for backup export. No analytics/advertising purpose, independent accreditation, provider ephemeral processing or automatic deletion promise. The optional deletion-mechanism claim remains unanswered.

Owner confirmed iFastNet hosts youriptv.us. Production source has no embedded WebView or developer telemetry endpoint; `BrandAssets.REMOTE_BRAND_ENDPOINT` is empty. The website URL is displayed as plain About text and in-app privacy content is bundled offline. General website visit statistics therefore do not establish an automatic app-data flow. The app/SDK code audited here does not infer location from IP or collect persistent device identifiers. Source servers necessarily see requests/IP, as the policy discloses; no unverified claim about arbitrary provider retention or analytics is made. Reassess if an actual controlled app backend/SDK/location inference is introduced.

The detailed assessment below explains the submitted source-flow profile.

## Main answers

| Console question | Answer for this build | Basis / scope |
|---|---|---|
| Does the app collect or share required user data types? | **Yes.** Do not choose No collection merely because there is no developer analytics server. | Source credentials/account identifiers and requested media resources leave the device for selected servers. |
| Is app-controlled user-data transmission encrypted? | **Yes for the audited HTTPS-only Play transport.** | Production HTTP/downgrades are blocked; certificate validation remains. Reconfirm the final AAB audit and any additional organizational/SDK flow before attesting to the complete form. Manual exports and other apps have separate handling. |
| Does the app let users create accounts? | **No YourIPTV app-account creation.** | Adding an existing provider login does not create a YourIPTV account. No developer cloud registration endpoint exists. |
| Optional deletion-mechanism claim | **Unanswered in the submitted form.** | Local deletion controls and the public policy remain. No unverified mailbox operation or deletion of independent-provider records is promised by a Play badge. |
| Independent security review/accreditation | **No accreditation claimed.** | Local unit/lint/artifact tests are not a Google-recognized independent security certification. |
| Ads / advertising purpose | **No.** | No ad SDK, ad display or advertising-ID access identified. |
| Analytics purpose / automatic crash reporting | **No for the audited app.** | No telemetry/crash SDK or developer collection endpoint identified. Do not infer what an unrelated provider does with its records. |

## Data-type entries

Use the following supported profile. The sharing column does not presume every third-party transfer is exempt. A documented exception can change a form answer, but not the disclosed actual flow.

| Data type | Collected | Shared entry | Purpose / required choice |
|---|---|---|---|
| Personal info: User IDs | **Yes** for authenticated sources. | **Yes as the conservative profile** for provider/account recipients; use an exception only after documenting the exact user-initiated/disclosed flow. | App functionality. Account management is not selected: Google's purpose concerns accounts with the developer, while these are external source accounts. Optional app-wide because anonymous sources are supported; required when the user chooses an authenticated source. Credentials can occur in URLs. |
| App activity: App interactions | **Yes** for requested catalogue/media identifiers and authenticated selection requests. | **Yes in the conservative profile** for selected media/artwork/guide recipients, unless a documented exception applies. | App functionality, not developer analytics. Requests are necessary for network playback, not separately disableable telemetry. |
| Device or other IDs / location derived from IP | **Not selected for the audited app implementation.** | No corresponding app/SDK identification/location flow found. | Servers see connection IP/user-agent. The app does not read advertising ID, Android ID, IMEI or GPS, and does not infer location. IP is classified by actual use; plain website text is not embedded website telemetry. Reassess actual new integrations rather than invent unrelated provider behavior. |
| Files/docs or exported user content | **Yes,optional**, covering explicit cloud/document export; local-only data is not off-device collection. | **Not shared** under the documented user-initiated destination-selection exception for explicit backup export. This exception is not applied to every transfer. | Optional restore/export functionality; not ephemeral,App functionality. Readable backup credentials are also in the User IDs flow. No automatic developer backup upload. |
| Email/name/messages sent to support | No app email field or embedded support uploader; voluntary email uses a selected mail app. | Assess any actual developer support integration added later. | Support correspondence is covered by the public policy. If a provider username is an email address, account for that identifier in the final form. |
| Precise location, contacts, health, financial/payment data, calendar, microphone/camera captures | **No corresponding collection in the audited app.** | No corresponding app transfer identified. | No relevant permission, billing flow or API use identified. Do not add unsupported categories merely because arbitrary user text could contain them. |
| Search history, favorites/history/PIN state | Local storage by default, not developer telemetry. | Only deliberate backup/external actions as described. | Local-only state is not off-device collection. A future remote-search/telemetry service would need reassessment. |

Do not mark provider processing ephemeral without evidence that the actual recipient retains it only in memory for the immediate request. The app cannot certify arbitrary server log retention. The supported conservative profile does **not** claim ephemeral provider processing. No advertising, marketing or analytics purpose should be selected without a real implementation/practice.

## Sharing and final verification

Google permits defined exceptions for service providers acting only for the developer, qualifying user-initiated/disclosed transfers and other specific cases. An independent source operator is not automatically the developer's service provider. Manual source selection/export differs from scheduled refresh and source-referenced artwork; review each flow instead of applying a universal exception.

The source-flow review found no mandatory developer backend, automatic website request or embedded web content. The separate IPTV service website was not inspected or assumed part of this app. A user-configured source can be controlled by a different operator; the declared credential/resource requests and policy disclose that flow without a blanket ephemeral-processing promise. Match answers with the public Play-inclusive policy and reassess new integrations. Live Console fields are authoritative; no import CSV that could overwrite existing answers is supplied.

No YourIPTV registration system exists, so app-account-creation deletion duties are not triggered by such a system. Playlist deletion, clear history, storage removal and admin@youriptv.us requests are documented; external source-account deletion belongs to its operator. [User Data/deletion policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en).
