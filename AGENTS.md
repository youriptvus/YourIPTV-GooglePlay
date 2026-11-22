# YourIPTV development rules

- YourIPTV-GooglePlay is the permanent development master. Make application changes only on its main branch.
- YourIPTV is the customer distribution repository, updated one way by the approved hosted synchronization workflow after the owner manually pushes master main. Never develop an independent application there.
- Local work only: do not connect to GitHub through browser, CLI, API or workspace tools, even read-only. The owner also configures authentication and Actions settings/secrets manually.
- The owner handles all GitHub pushes, source uploads and public releases. Development tools must never push or upload repository content directly. Only the explicitly approved hosted source-sync workflow may write the secondary repository.
- Local shell, Git inspection/branches/commits, Python, Gradle, Android tools and authorized device testing are allowed. Preserve uncommitted work, Git history, package identity and existing signing identities. Never force-push.
- Android phones and Android TV remain one application, com.youriptv.app, sharing app/src/main. Select delivery with distribution=play or customer; keep only necessary distribution hooks separate.
- Preserve playback, playlist/EPG, history/resume/autoplay, navigation, parental controls and existing saved settings. Settings Lock defaults off; preserve an existing explicit setting.
- Version 1.1.1/code52 and its published closed test are protected during this migration. Do not rebuild, upload, replace or republish that release, alter testers/links/tracks or change Console releases.
- Future releases require explicit owner authorization. Set version.properties once for both distributions. Use scripts/prepare_release.py to prepare independently signed AAB/APK packages and matching metadata in separate immutable version folders outside the repository.
- Never use debug signing for a production candidate or assume an upload key is the Play-delivered signing key. Preserve the customer updater contract and direct upgrades where compatible.
- Keep source synchronization, local release preparation and publication separate. No workflow publishes application packages or writes to Play Console.
- Never include private keys, signing properties, tokens, provider/customer credentials, device identifiers, test evidence or confidential handoffs in public source or archives. Audit pinned source and exports with externally supplied restricted terms; missing configuration fails closed.
- Public materials must not name competing IPTV applications. Keep the restricted vocabulary exclusively in private configuration/Actions secrets, including in tests and scripts. Do not rewrite historic commits or delete historic releases.
- Consult docs/DEVELOPMENT_WORKFLOW.md and docs/SECURITY_AND_SIGNING.md before synchronization or release work. Report actual validation and limitations; do not claim remote Actions execution before the owner pushes and it runs.
