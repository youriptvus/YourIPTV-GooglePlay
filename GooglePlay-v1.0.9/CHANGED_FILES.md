> Current final delivery (November 18): GooglePlay-Release-Ready/START-HERE.md and Reports/Final-Delivery-Report.md are authoritative. Signed artifacts, current phone/TV/public HTTPS/ATF checks and public privacy verification passed; owner Console/signing/declarations/testing/approval gates remain. Earlier phase numbers and pending-hosting statements below are historical.

# Source changes from the official baseline

> **Current completion:** the earlier inventory below describes its recorded phase. Current resource/contact/artwork and release-package changes are recorded by the new [completion reports](../GooglePlay-Release-Ready/Reports/); do not treat 295 as the final new source count. The pinned 234-file original inventory is unchanged.

The current HTTPS-only secure candidate is being prepared separately under `artifacts/code48-secure/`; final secure tests/artifact results are pending. The 401-test HTTP-integrity build and first 81-check code 48 artifact audit predate this transport change and are intermediate/superseded. Root-level code 45 evidence remains historical. See [current secure status](CURRENT_STATUS.md).

## Code 48 continuation: Play 1.0.9

The intermediate HTTP-integrity build source was `2c02256822cfc8c9c6b0436cceaeb1f4e7b3830a`; final secure production source is `cc13ebe5bba6dfba1fd00d38e976ce2c1756e4f3`, with host/verifier/lint checks passed and final compiled-artifact verification pending. VersionName remains 1.0.9; local versionCode 48 is proposed pending unused Console-history verification. The narrow HTTP integrity fix counts raw encoded bytes and rejects incomplete advertised-length responses, including gzip completion. Its PC/JVM scope and retained TV/provider limitations are in [CURRENT_STATUS.md](CURRENT_STATUS.md) and [machine-readable status](CURRENT_STATUS.json).

The continuation adds or updates these implementation/version-verification paths:

- `app/src/main/java/com/youriptv/app/data/ContentLengthInputStream.kt`: new raw encoded-length guard.
- `app/src/main/java/com/youriptv/app/data/Http.kt`: integrate the guard around the encoded response before decompression.
- `app/src/test/java/com/youriptv/app/HttpContentLengthTest.kt`: 16 focused HTTP integrity regression cases.
- `app/src/test/java/com/youriptv/app/PlayDistributionAuditTest.kt`: verify proposed versionCode 48.
- `app/build.gradle.kts`: retain versionName 1.0.9 and use local proposed versionCode 48.
- `scripts/verify_play_candidate.py`: default verification expectations for proposed code 48.
- `CHANGELOG.md` and `docs/RELEASE_NOTES.md`: synchronized current Play notes, with the imported customer history preserved.

The HTTPS-only compliance continuation additionally adds:

- `app/src/main/java/com/youriptv/app/data/SecureTransport.kt`: shared secure-source/redirect policy.
- `app/src/main/java/com/youriptv/app/ui/SecureDataSourceFactory.kt`: secure media data-source boundary.
- `app/src/isolatedTest/res/xml/network_security_config.xml`: isolated local-fixture configuration; production cleartext remains blocked.
- `app/src/test/java/com/youriptv/app/SecureTransportTest.kt`: secure transport and input regressions.
- `scripts/tests/test_verify_play_candidate.py`: compiled network-policy verifier regressions.

Related existing paths updated for HTTPS enforcement and messaging are the production manifest/network XML, `Http.kt`, `StreamDiagnosis.kt`, `Nav.kt`, `PlaybackFactory.kt`, `PlaylistEditActivity.kt`, `PublicProviderTest.kt` and `scripts/verify_play_candidate.py`. The bundled `app/src/main/res/values/privacy.xml`, privacy/Data safety/HTML drafts, listing and migration guidance explain HTTPS-only connections and legacy HTTP URL replacement. Exact full source counts await the final secure audit; no new device pass is implied.

Current documentation also adds `CURRENT_STATUS.md` / `CURRENT_STATUS.json` and links them from active README, handoff, baseline, signing, Console, checklist, maintenance, build and test guidance. The code 48 delivery/source archive is prepared separately under `artifacts/code48-secure/`; root-level code 45 artifacts and historical JSON remain unchanged. This continuation does not change the original customer repository or add a device test pass. Final current source counts will be recorded after the independent artifact/source audit; the prior counts below are not the code 48 inventory.

## Historical code 45 documentation-freeze inventory

Baseline: official v1.0.9, commit `0612c0acb500f096da5cfb766f6e7f0a012c4ba8` (234 files). This inventory covers the independent source/docs/test/QA-image tree; it excludes ignored build output, delivery artifacts, private configuration and the unrelated nested checkout. Text comparison normalizes CRLF/LF. Production build source is `f5a421823aa94fc8f652edc52050b8f12f5d7130`; later documentation and test evidence do not change that AAB.

Historical code 45 documentation-freeze snapshot: **42 changed, 13 removed, 64 added**; 179 baseline files unchanged; 285 current source files. The 14 actual QA captures and their index are included.

## Changed existing paths

- `.gitignore`
- `CHANGELOG.md`
- `README.md`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/youriptv/app/YourIptvApp.kt`
- `app/src/main/java/com/youriptv/app/data/Parental.kt`
- `app/src/main/java/com/youriptv/app/data/Settings.kt`
- `app/src/main/java/com/youriptv/app/data/SettingsLockPolicy.kt`
- `app/src/main/java/com/youriptv/app/data/XtreamUrls.kt`
- `app/src/main/java/com/youriptv/app/ui/AboutActivity.kt`
- `app/src/main/java/com/youriptv/app/ui/AudioDelayProcessor.kt`
- `app/src/main/java/com/youriptv/app/ui/Branding.kt`
- `app/src/main/java/com/youriptv/app/ui/CustomerSettingsPanel.kt`
- `app/src/main/java/com/youriptv/app/ui/EpgSettingsPanel.kt`
- `app/src/main/java/com/youriptv/app/ui/LiveTsExtractors.kt`
- `app/src/main/java/com/youriptv/app/ui/PlayerActivity.kt`
- `app/src/main/java/com/youriptv/app/ui/PlaylistEditActivity.kt`
- `app/src/main/java/com/youriptv/app/ui/PlaylistsActivity.kt`
- `app/src/main/java/com/youriptv/app/ui/RemoteActivity.kt`
- `app/src/main/java/com/youriptv/app/ui/RemoteBack.kt`
- `app/src/main/java/com/youriptv/app/ui/SettingsActivity.kt`
- `app/src/main/java/com/youriptv/app/ui/UiUtil.kt`
- `app/src/main/res/layout/activity_about.xml`
- `app/src/main/res/layout/activity_movie_details.xml`
- `app/src/main/res/layout/activity_player.xml`
- `app/src/main/res/layout/activity_playlist_edit.xml`
- `app/src/test/java/com/youriptv/app/BackupTest.kt`
- `app/src/test/java/com/youriptv/app/CatchupTest.kt`
- `app/src/test/java/com/youriptv/app/DataLogicTest.kt`
- `app/src/test/java/com/youriptv/app/PlaylistEditingTest.kt`
- `app/src/test/java/com/youriptv/app/SettingsLockPolicyTest.kt`
- `app/src/test/java/com/youriptv/app/SettingsPinPolicyTest.kt`
- `app/src/test/java/com/youriptv/app/StandaloneAppAuditTest.kt`
- `docs/QA_CHECKLIST.md`
- `docs/RELEASE_NOTES.md`
- `docs/SETTINGS_SECURITY.md`
- `docs/UPDATE_SERVER.md`
- `docs/UPGRADING_TO_1_0.md`
- `gradle.properties`
- `keystore.properties.example`
- `settings.gradle.kts`

## Removed baseline paths

- `app/src/main/java/com/youriptv/app/ui/AppUpdateActivity.kt`
- `app/src/main/java/com/youriptv/app/ui/YourIptvLogin.kt`
- `app/src/main/java/com/youriptv/app/updates/AppUpdateChecks.kt`
- `app/src/main/java/com/youriptv/app/updates/AppUpdates.kt`
- `app/src/main/java/com/youriptv/app/updates/InstalledChangelog.kt`
- `app/src/main/java/com/youriptv/app/updates/UpdateReceipt.kt`
- `app/src/main/java/com/youriptv/app/updates/UpdateRelease.kt`
- `app/src/main/java/com/youriptv/app/updates/UpdateTransport.kt`
- `app/src/test/java/com/youriptv/app/AppUpdatesTest.kt`
- `app/src/test/java/com/youriptv/app/UpdateReceiptTest.kt`
- `app/src/test/java/com/youriptv/app/UpdateTransportTest.kt`
- `app/src/test/java/com/youriptv/app/YourIptvLoginTest.kt`
- `scripts/make_update_json.py`

## Added paths

- `.gitattributes`
- `.github/workflows/verify.yml`
- `AGENTS.md`
- `GooglePlay-v1.0.9/CHANGED_FILES.md`
- `GooglePlay-v1.0.9/HANDOFF.md`
- `GooglePlay-v1.0.9/PLAY_CONSOLE_INSTRUCTIONS.md`
- `GooglePlay-v1.0.9/PLAY_POLICY_AUDIT.md`
- `GooglePlay-v1.0.9/PLAY_STORE_ASSETS.md`
- `GooglePlay-v1.0.9/PLAY_STORE_CHECKLIST.md`
- `GooglePlay-v1.0.9/PLAY_STORE_DATA_SAFETY.md`
- `GooglePlay-v1.0.9/PLAY_STORE_LISTING.md`
- `GooglePlay-v1.0.9/PLAY_STORE_PRIVACY_DRAFT.md`
- `GooglePlay-v1.0.9/PLAY_STORE_README.md`
- `GooglePlay-v1.0.9/PLAY_STORE_RELEASE_NOTES.md`
- `GooglePlay-v1.0.9/PLAY_STORE_REVIEW_ACCESS.md`
- `GooglePlay-v1.0.9/PLAY_STORE_SIGNING.md`
- `GooglePlay-v1.0.9/PLAY_STORE_TEST_REPORT.md`
- `GooglePlay-v1.0.9/REPOSITORY_SEPARATION.md`
- `GooglePlay-v1.0.9/SOURCE_BASELINE.md`
- `GooglePlay-v1.0.9/SOURCE_FILE_INVENTORY.json`
- `GooglePlay-v1.0.9/privacy-policy-draft.html`
- `GooglePlay-v1.0.9/screenshots/README.md`
- `GooglePlay-v1.0.9/screenshots/phone-samsung-playtest-about.png`
- `GooglePlay-v1.0.9/screenshots/phone-samsung-playtest-post-restart.png`
- `GooglePlay-v1.0.9/screenshots/phone-samsung-playtest-post-same-code-replacement.png`
- `GooglePlay-v1.0.9/screenshots/phone-samsung-playtest-privacy.png`
- `GooglePlay-v1.0.9/screenshots/phone-samsung-playtest-settings.png`
- `GooglePlay-v1.0.9/screenshots/phone-samsung-playtest-synthetic-guide.png`
- `GooglePlay-v1.0.9/screenshots/phone-samsung-playtest-synthetic-player.png`
- `GooglePlay-v1.0.9/screenshots/screenshots.json`
- `GooglePlay-v1.0.9/screenshots/tv-onn-playtest-about.png`
- `GooglePlay-v1.0.9/screenshots/tv-onn-playtest-post-restart.png`
- `GooglePlay-v1.0.9/screenshots/tv-onn-playtest-post-same-code-replacement.png`
- `GooglePlay-v1.0.9/screenshots/tv-onn-playtest-privacy.png`
- `GooglePlay-v1.0.9/screenshots/tv-onn-playtest-settings.png`
- `GooglePlay-v1.0.9/screenshots/tv-onn-playtest-synthetic-guide.png`
- `GooglePlay-v1.0.9/screenshots/tv-onn-playtest-synthetic-player.png`
- `HANDOFF.md`
- `SOURCE_BASELINE.md`
- `app/src/androidTest/AndroidManifest.xml`
- `app/src/androidTest/java/com/youriptv/app/PlaySmokeInstrumentation.kt`
- `app/src/androidTest/java/com/youriptv/app/TrialPlaybackInstrumentation.kt`
- `app/src/isolatedTest/AndroidManifest.xml`
- `app/src/main/java/com/youriptv/app/ui/PrivacyActivity.kt`
- `app/src/main/res/values-port/player_dimensions.xml`
- `app/src/main/res/values/mobile_strings.xml`
- `app/src/main/res/values/player_dimensions.xml`
- `app/src/main/res/values/privacy.xml`
- `app/src/main/res/xml-v24/backup_rules.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/test/java/com/youriptv/app/AdaptiveUiResourceTest.kt`
- `app/src/test/java/com/youriptv/app/PlayDistributionAuditTest.kt`
- `app/src/test/java/com/youriptv/app/PublicProviderTest.kt`
- `docs/PLAY_BUILD_AND_TEST.md`
- `docs/PLAY_MAINTENANCE.md`
- `docs/PLAY_POLICY_AUDIT.md`
- `scripts/DumpBundleXml.java`
- `scripts/export_review_demo.py`
- `scripts/private-trial.init.gradle`
- `scripts/run_device_smoke.py`
- `scripts/run_private_trial.py`
- `scripts/run_review_fixture.py`
- `scripts/verify_play_candidate.py`
