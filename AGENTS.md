# Independent Play project rules

The sibling customer repository is read-only. Never change its files, Git metadata, workflows, releases, updater or settings. No shared Git directory, customer worktree, fork or automatic synchronization.

- One production `com.youriptv.app`, shared player/version and AAB for phone/tablet/TV. `.playtest` is temporary isolated QA only, never another production listing/AAB.
- Preserve v1.0.9 TS recovery, Right previous-channel/retries, provider limits, parental restrictions and authorized Admin functions.
- Fresh Settings Lock OFF; preserve saved/ambiguous existing security. Missing custom PIN is not proof of an old default.
- Play delivers updates. No APK installer/downloader/customer updater routes or installer overlay permissions.
- Generic authorized user sources only; no subscription-provider shortcuts, credentials or unauthorized streams. No paywalls/timers/trials.
- Never commit/upload keys, private config/passwords, customer databases, backups or sensitive logs/screenshots. External signing keys/config must stay outside Git. Never print secrets. Use `py` for Python.
- Work locally only under the owner's latest explicit instruction. Do not call GitHub, read remote state, create a remote, push or upload anything from workspace tools. The owner handles any private-repository creation, verification and push manually. Retain the already configured new-only origin locally. Obsolete comp6062 remains prohibited.
- Local commits, builds, tests and authorized isolated LAN/USB device checks are allowed. No GitHub Release or Play submission. Do not start a GitHub authentication/connection flow to finish the current task.
- For any later owner-run publication, review secrets and verify the exact PRIVATE non-fork destination first. No remote-private claim without actual owner-supplied server evidence.
- Code45 proposed pending Console upload-history check. If used, choose higher unused code and rebuild/retest/re-hash; no code44 reuse.
- No automatic production-key generation/replacement or Play Signing enrollment. Owner decides signing compatibility. Unsigned release preparation permitted.
- Never clear/uninstall/replace customer apps/data, delete playlists, alter PINs/launcher or factory reset for QA. Verify identity and use isolated QA/emulator. Sound assumed unless an actual error/report.
- Report exact tests and limitations: static/JVM, isolated device and actual production/Play delivery differ. Never mark unperformed tests passed.
- Current instructions in GooglePlay-v1.0.9 and docs/PLAY_*. Historical customer evidence does not certify Play. Update reports/hashes after every candidate change.
