# Play upgrade/migration

Production package remains com.youriptv.app; cannot ordinarily coexist with customer package. Ordinary upgrade needs compatible certificate and higher Android code. Separate Git history or differently signed AAB/debug build does not establish compatibility.

Never uninstall/clear/replace customer data to test. Use isolated .playtest/emulators for fresh state. Preserve saved credentials/playlists/preferences/parental restrictions/lock state. Empty fresh prefs=>Settings OFF; preexisting prefs with missing choice=>conservative ON; saved choices retained. Legacy Admin path requires authentication before local enrollment.

Owner must verify official certificate against Play App Signing and test actual Play-generated compatible update/data retention in an authorized internal track. Proposed45 awaits all Console upload history. No intermediate customer release required; custom previousVersionCode metadata is irrelevant to Play.

See GooglePlay-v1.0.9 signing/Console docs. Customer updater/source remains unchanged.
