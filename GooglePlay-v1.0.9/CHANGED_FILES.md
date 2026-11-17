# Source changes from the official baseline

Baseline: official v1.0.9, commit `0612c0acb500f096da5cfb766f6e7f0a012c4ba8` (234 files). This inventory covers the independent source/docs/test/QA-image tree; it excludes ignored build output, delivery artifacts, private configuration and the unrelated nested checkout. Text comparison normalizes CRLF/LF. Production build source is `f5a421823aa94fc8f652edc52050b8f12f5d7130`; later documentation and test evidence do not change that AAB.

Final documentation-freeze snapshot: **42 changed, 13 removed, 64 added**; 179 baseline files unchanged; 285 current source files. The 14 actual QA captures and their index are included.

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
