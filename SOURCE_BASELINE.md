# Source baseline and current release

Current maintained local candidate: **YourIPTV 1.1.1 / code 52**, `com.youriptv.app`, minimum SDK 24 and target/compile SDK 36. Source/build identities for this amendment must come from its final refreshed commit/artifact sidecars; the original import commit below is provenance, not the current source binding.

## Original independent import

The independent Play project was imported on November 16, 2026 from the official customer 1.0.9/code44 source at recorded commit `0612c0acb500f096da5cfb766f6e7f0a012c4ba8`. The historical record states 234 tracked source files were copied without customer Git history, signing material, credentials, local configuration, caches or build output. The project retains separate Git history and release packaging; it is not an attached customer worktree/submodule. Earlier remote-release observations do not establish today's remote state or canonical tag.

Customer repository identity: `youriptvus/YourIPTV`. Independent Play project identity: `youriptvus/YourIPTV-GooglePlay`. These names identify intended projects; this local update makes no GitHub request or current visibility/content assertion. Publication/push remains manual.

## Maintained feature and distribution boundaries

Both requested release variants retain shared player/navigation/history behavior: grouped series history with parent artwork and last-watched episode, episode-level resume/Continue Watching, thin green progress, manual watched/reset precedence, finite autoplay across seasons, catch-up/source guards, remembered live startup, held Back fullscreen return and guide current-time entry. Existing history is preserved without a schema migration.

Settings Lock defaults OFF while saved choices/security remain respected. Autoplay defaults ON only when unconfigured, retains explicit OFF and is available in normal Playback settings. Admin and parental controls remain independent.

The Play build uses external signing configuration, Play-managed updates and no APK self-installer/customer update endpoints or bundled customer-provider shortcut. One app/AAB supports phone/tablet/TV. User-entered HTTP and HTTPS sources preserve their schemes; TLS certificate validation and HTTPS-to-HTTP downgrade protection remain. The historical HTTPS-only transport description is superseded for code 52.

## Current evidence and outstanding publication facts

Final isolated-device QA reports 185 checks passed across Moto API 36 and onn. TV API 34, with production identities preserved; see `D:\APK\work\YourIPTV-history-device-qa-20261121T163606Z\watch-guard-final\final-summary.json` and [build/test guidance](docs/PLAY_BUILD_AND_TEST.md) for provenance and limitations. Use final build sidecars for release hashes, test totals and pinned source binding.

Before owner publication, verify unused code 52 across current Console history, app-signing compatibility, privacy/Data safety, real protected delivery, current tester/production gates and remote canonical source/tag. No approval or publication is claimed. The prior baseline/status wording is preserved as [dated history](docs/history/2026-11-21-pre-1.1.1-refresh/SOURCE_BASELINE.md).
