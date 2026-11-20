# Authorized generated reviewer media

The public playlist https://youriptv.us/review-media/playlist.m3u and rolling guide https://youriptv.us/review-media/epg.php were independently verified over HTTPS on November 18, 2026. Actual phone and TV public-source results are in Reports/Accessibility-and-Public-Playback.md. This continuation performed no deployment. The earlier hosting receipt is retained in Reports/Public-Hosting-Verification.json.

The two six-second MPEG-TS clips are locally generated testsrc2 video and sine audio. They contain no television, movie, subscription-provider or customer material. The four playlist entries demonstrate two live items, a movie and a series episode. The APK/AAB contains none of this demo media or a default playlist.

The public playlist uses epg.php, whose prepared local source is included here. It returns a rolling eight-day XMLTV window without credentials. epg.xml is a static offline fallback and becomes stale; it is not the public playlist's guide. Regenerate-Demo.py refreshes only local fallback metadata, never uploads anything and keeps the dynamic guide address. Changing the public base requires a new availability check.

Keep these public endpoints operational throughout review. No private account is needed for these generated examples. A separate authorized reusable account may be required for source-restricted Xtream/catch-up features that the generated demo cannot show; put secrets only in Play Console. Public checks from this connection do not prove every country or future availability.
