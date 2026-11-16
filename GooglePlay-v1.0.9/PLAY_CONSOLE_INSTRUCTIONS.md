# Owner Console preparation and submission

Researched November16,2026. Owner access/decisions needed; do not upload/publish automatically.

1. Register https://play.google.com/console if needed; choose accurate personal/organization identity, pay current US$25 one-time fee and complete requested identity/contact/payment/device verification. Organization evidence differs. https://support.google.com/googleplay/android-developer/answer/6112435?rd=1
2. Check account owns/has no conflict for com.youriptv.app. Create ONE application entry if appropriate, no separate phone/TV listing. Resolve existing package/history first.
3. Check App bundle explorer and all tracks/drafts/prior uploads for code45. Record unused proof or select higher unused and rebuild/retest/re-hash.
4. Review Play App Signing BEFORE rollout. Owner selects compatible existing-key/PEPK enrollment and upload key separately. Do not accept incompatible generated signing identity by default. See signing doc.
5. Obtain verified signed unified AAB/hash/source commit. One bundle serves all form factors; generated APK splits expected. Unsigned output not upload-ready.
6. With explicit authorization create internal-test release; inspect warnings/device catalog/API/ABI/hardware. Validate generated certificate, fresh/offline source playback, safe updates/data retention. No automatic overwrite of customer app.
7. Complete listing: name/category/support, copy, approved512px icon,1024x500 feature graphic and actual screenshots; TV1280x720 store banner/current TV screenshot. Phone/tablet imagery must reflect real supported UI.
8. Publish matching privacy HTML with real publisher/contact; public stable non-geofenced non-editable, no PDF/login. Enter Console URL; align in-app notice.
9. Complete Data safety from actual transfers/SDKs, content rating, Ads (no ad SDK in audited candidate), target audience and required permission/FGS declarations. HTTP means unconditional encrypted-in-transit answer unsupported.
10. App access: stable legal demo and full-feature reviewer account if needed, exact steps, no customer credentials/expiring OTP/geofence/LAN dependency. Check features beyond simple MP4 demo.
11. Opt into Android TV; current quality review includes launchers/focus/Back/banner/landscape,32+64-bit/16KB and device catalog. Mobile approval does not imply TV. https://developer.android.com/training/tv/publishing/distribute
12. Internal testing then closed test as needed. Personal accounts created after November13,2023: at least12 testers opted in continuously14 consecutive days, then APPLY for production access and answer readiness questions. No automatic access approval. Open testing follows production access. https://support.google.com/googleplay/android-developer/answer/14151465?hl=en
13. Test actual Play delivery on phone/tablet/TV and relevant Android versions: security, source features, limits, focus/touch/fullscreen/system bars, lifecycle, prolonged playback/repeated interruption and compatible updates.
14. Review declarations/artifacts/countries/rollout/notes and obtain explicit owner production-submission authorization. Submit/review feedback, fix here with higher unused code where needed; claim availability only after Google approval.

GitHub cannot prove account type/package ownership/Console unused codes/production access/Play signing. These remain owner checks.
