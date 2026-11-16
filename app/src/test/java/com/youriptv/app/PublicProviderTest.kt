package com.youriptv.app

import com.youriptv.app.data.SourceType
import com.youriptv.app.data.Playlist
import com.youriptv.app.data.XtreamUrls
import com.youriptv.app.data.ConnectionLimit
import com.youriptv.app.data.BackupKeys
import com.youriptv.app.data.PinVault
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PublicProviderTest {
    @Test fun schemeLessNewProvidersPreferHttpsAndExplicitHttpIsPreserved() {
        assertEquals("https://provider.example:8080", XtreamUrls.normalizeBase("provider.example:8080/"))
        assertEquals("http://provider.example:8080", XtreamUrls.normalizeBase("http://provider.example:8080/"))
        assertEquals("https://provider.example", XtreamUrls.normalizeBase("https://provider.example/player_api.php"))
    }

    @Test fun userSuppliedCredentialsAreEncodedForProviderRequests() {
        assertEquals("https://provider.example/player_api.php?username=user%2Bname&password=p%26word&action=get_live_streams",
            XtreamUrls.api("provider.example", "user+name", "p&word", "get_live_streams"))
    }

    @Test fun providerLimitsStillApplyAcrossMatchingXtreamAndM3uConnections() {
        val xtream = Playlist(1L, "Authorized provider", SourceType.XTREAM, "https://provider.example", "demo", "synthetic", maxConnections = 1)
        val m3u = Playlist(2L, "User playlist", SourceType.M3U, "https://provider.example/get.php?username=demo&password=synthetic")
        assertEquals(1, ConnectionLimit.effective(m3u, listOf(xtream, m3u)))
        assertTrue(ConnectionLimit.sameAccount(xtream, m3u))
    }

    @Test fun adminCredentialsAndMigrationAuthorityCannotBeImportedOrExported() {
        val attempted = mapOf("admin_pin_hash" to "synthetic", "admin_pin_salt" to "synthetic", PinVault.LEGACY_ADMIN_KEY to "true")
        assertTrue(BackupKeys.sanitize(attempted).isEmpty())
    }

    @Test fun productionEditorContainsNoBundledProviderShortcutAndWarnsBeforeHttp() {
        val main = listOf(File("src/main"), File("app/src/main")).first { it.exists() }
        val editor = File(main, "java/com/youriptv/app/ui/PlaylistEditActivity.kt").readText()
        assertFalse(File(main, "java/com/youriptv/app/ui/YourIptvLogin.kt").exists())
        assertFalse(editor.contains("yourIptvMode"))
        assertTrue(editor.contains("Unencrypted provider connection"))
        assertTrue(editor.contains("insecureConfirmedFor = null"))
        assertTrue(editor.contains("authorization.isCurrent(visit) && authorization.isAuthorized"))
    }
}
