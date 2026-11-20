> Current November19 candidate:1.0.9/code49,minSdk24/target36, automatic protection ON. See Reports/Protected-Candidate-Report.md and Protected-Candidate-Verification.json. Earlier code48 build/device counts below are historical; code49 has fresh host/artifact verification but no fresh protected Play-delivery pass. The owner authorized Console signing/test-draft operations.

# Current full device regression results

| Actual isolated device | Checks | Failures | Restart and same-code replacement |
|---|---:|---:|---|
| R5GL75PGMFP | 35 | 0 | Passed |
| 192.168.1.150:5555 | 32 | 0 | Passed |

Both suites ran against current shared production UI/player code through com.youriptv.app.playtest, with a 180-second synthetic live-recovery observation. They exercise Settings OFF/Admin/parental/lockouts, provider limits, catalogue/guide refresh, D-pad navigation, RIGHT recall/overlay precedence, lifecycle, movie/episode decoded-frame advancement through STATE_ENDED and Activity finish. The Samsung includes touch and portrait/landscape recreation. Public HTTPS and Google ATF/keyboard results are separately recorded in Accessibility-and-Public-Playback.md.

Real QA process restart and exact same-code reinstall preserve the saved OFF flag and active playlist ID. This is not a production/Play-generated or version-increasing update test. Customer package version/install metadata was unchanged on both devices; customer apps were never installed over, cleared or uninstalled. All 1124 customer-repository files and the complete file set remain unchanged.

Wake-only input every ten seconds maintained foreground tests without saved power changes. Historical HDMI standby failures are retained. Two current phone attempts passed 34/35: targeted touch injection was rejected outside the QA window, then the explicit active-window guard refused the tap. Waiting for the newly CLEAR_TASK-launched activity to own input, together with visible-rectangle targeting, passed the complete suite. Earlier TV report extraction failed because scoped external storage was unavailable to shell in the active profile, although instrumentation reported 14/14; test evidence now uses app-private files. A fresh QA install was made only in active TV profile 0 after confirming no prior data directory there; profile 10 was never launched/switched and its never-launched QA state was preserved.

Limits: generated bounded clips and injected input; sound assumed, no manual TalkBack/switch-access, physical tablet/16 KB hardware, physical remote timing, prolonged private-provider playback or Google Play delivery claim. The QA loopback exception is absent from the audited HTTPS-only production build. Full failed attempts remain at D:\APK\work\YourIPTV-GooglePlay-completion-20261118T030147Z-a244466f\resume-20261118T223615Z and the earlier completion run; they are not relabeled as passes.
