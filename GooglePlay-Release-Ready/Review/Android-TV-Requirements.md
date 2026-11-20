# Android TV / Google TV submission

Use the same com.youriptv.app application and production AAB. The source declares phone LAUNCHER and LEANBACK_LAUNCHER, optional touchscreen/leanback/landscape hardware, an in-app banner and the shared player. min SDK 23 is within the TV requirement of min SDK 31 or lower; target SDK 36 meets the stricter unified-mobile requirement.

TV quality includes five-way/D-pad navigation, visible focus, usable Back/Home flow, readable fullscreen landscape UI, lifecycle/media behavior and accurate advertised functions. Manifest declarations do not certify usability. Final physical/emulator observations and gaps belong in Reports/Test-Report.md. Do not claim physical-remote/audio timing, tablet hardware or prolonged endurance without evidence.

TV 32/64-bit and 16 KB support applies from August 1, 2026. The previous secure audit found no packaged .so libraries; Java/Kotlin-only packaging is architecture-neutral. Recheck final dependencies/artifacts. A native scan is not a physical 16 KB-device run.

In the existing app entry, opt into Android TV under Setup -> Advanced settings -> Form factors, add Android TV and accept review requirements. Use the same TV-capable bundle; a dedicated TV track does not require another package/app. Add the 1280x720 TV store banner and at least one accurate current TV screenshot. The 320x180 in-app launcher banner is a different asset. Mobile approval does not confer TV approval.

Official references: [TV distribution](https://developer.android.com/training/tv/publishing/distribute), [TV quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality), [16KB support](https://developer.android.com/guide/practices/page-sizes), [preview assets](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en).

The current completion corrects the TV fallback launcher sizes using ten TV-qualified PNGs (80/120/160/240/320 px) and two v26 adaptive aliases. Phone resources are unchanged. Verify the compiled TV-selected variants in the final artifact audit. [TV icon/banner resource guide](https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines).
