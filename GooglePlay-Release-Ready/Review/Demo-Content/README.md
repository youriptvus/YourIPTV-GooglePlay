# Authorized synthetic YourIPTV reviewer demo

Status: OFFLINE HOSTING PREPARATION ONLY - no upload or public reachability has been verified.

Configured base: https://youriptv.us/review-media/

The two MPEG-TS files contain the same six-second synthetic testsrc2 pattern and sine audio generated locally with FFmpeg. No television programme, commercial channel, customer provider, account, logo or third-party movie is included. Media is outside application assets and is not bundled into the production AAB. The owner may use these generated demo files for authorized review.

Files to host together: playlist.m3u, epg.xml, channel1.ts and channel2.ts. The guide uses matching synthetic1/synthetic2 IDs and a rolling UTC window; regenerate daily while review access is active. The app reconnects when each finite TS sample ends. This fixture is useful for guide/live/toggle behavior, not a proof of Xtream, catch-up, Movies or Series access.

Before public use, select an owner-controlled stable HTTPS directory and run from the source root:

    py scripts/export_review_demo.py --reuse-media --public-base https://YOUR-APPROVED-HOST.example/youriptv-review/

Replace the example hostname before any upload. Uploading or deploying requires separate owner authorization. No deployment is performed by this script. Preserve HTTPS, correct MIME types, byte-range support where available, and access without a login, expiring link, geofence or home-LAN dependency. Verify all four public files from outside the home network and test the exact playlist on phone and TV before entering App access instructions.

Repeat the --reuse-media command daily, then deploy only the updated epg.xml through the approved hosting process. Confirm timestamps and reachability throughout Google's review. Never add customer credentials or subscription content to this folder.


This completion package also includes explicit M3U movie/episode entries. They reuse the same generated clips through harmless query strings; configure the static host to serve those URLs and support byte ranges. These short synthetic samples demonstrate player/category behavior, not a subscription catalogue or rich Xtream metadata. The proposed HTTPS path is not deployed or verified. No private account is required.
