# 1.1.1 / code52 publication requirements

The provider HTTP compatibility fix is prepared locally. The released code51 files and existing Play Console tracks are unchanged.

Use the existing app/package and upload signing key with automatic protection ON. Upload the new code52 AAB deliberately after verifying that the code remains unused. Publish the matching customer repository/release only after reviewing the local changes. No keys or protection settings need changing.

Publish the accompanying updated privacy policy at the existing public policy URL as part of this release. HTTP source requests may include provider credentials and are not encrypted; do not retain a blanket HTTPS-only or all-data-encrypted claim. Review the Play Data safety answers against the current collection/transfer inventory. If covered data may travel over HTTP, an all-data-encrypted-in-transit answer must be No. HTTP support is Android functionality, not a guarantee of Google approval; all User Data policy requirements still apply. User-selected source operators have independent privacy practices.

Official references: https://developer.android.com/media/media3/exoplayer/troubleshooting and https://support.google.com/googleplay/android-developer/answer/10787469 and https://support.google.com/googleplay/android-developer/answer/10144311

The Moto's Play-installed production app remains code51. The successful corrected imports/playback use the isolated QA package, not a modified Play-protected installation. The repaired production AAB still needs Play processing and delivery.
