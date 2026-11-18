# Android TV / Google TV submission

Use the same com.youriptv.app application and production AAB. The source declares phone LAUNCHER and LEANBACK_LAUNCHER, optional touchscreen/leanback/landscape hardware, an in-app banner and the shared player. minSdk23 is below the current TV maximum minimum of31; target36 meets the stricter unified-mobile requirement.

TV quality includes five-way/D-pad navigation, visible focus, usable Back/Home flow, readable fullscreen landscape UI, lifecycle/media behavior and accurate advertised functions. Manifest declarations do not certify usability. Final physical/emulator observations and gaps belong in Reports/Test-Report.md. Do not claim physical-remote/audio timing, tablet hardware or prolonged endurance without evidence.

TV32/64-bit and16KB support applies from August1,2026. The previous secure audit found no packaged.so libraries; Java/Kotlin-only packaging is architecture-neutral. Recheck final dependencies/artifacts. A native scan is not a physical16KB-device run.

In the existing app entry, opt into Android TV under Setup -> Advanced settings -> Form factors, add Android TV and accept review requirements. Use the same TV-capable bundle; a dedicated TV track does not require another package/app. Add the1280x720 TV store banner and at least one accurate current TV screenshot. The320x180 in-app launcher banner is a different asset. Mobile approval does not confer TV approval.

Official references: [TV distribution](https://developer.android.com/training/tv/publishing/distribute), [TV quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality), [16KB support](https://developer.android.com/guide/practices/page-sizes), [preview assets](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en).
