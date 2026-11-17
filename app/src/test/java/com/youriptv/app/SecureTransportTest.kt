package com.youriptv.app

import com.youriptv.app.data.InsecureTransportException
import com.youriptv.app.data.SecureTransport
import com.youriptv.app.data.StreamDiagnosis
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class SecureTransportTest {
    private val module = listOf(File("."), File("app")).first { File(it, "src/main/AndroidManifest.xml").exists() }
    private fun source(path: String) = File(module, "src/main/$path").readText()

    @Test fun configuredRemoteSourcesRequireAnAbsoluteHttpsHost() {
        for (url in listOf("https://provider.example", "HTTPS://provider.example:443/list?account=synthetic", "https://provider.example/guide.xml")) {
            assertTrue(url, SecureTransport.isHttpsUrl(url))
            SecureTransport.requireHttps(url)
        }
        for (url in listOf("http://provider.example", "ftp://provider.example/list", "file:///local/list", "https:///missing-host", "https://", "//provider.example", "provider.example", "https://provider.example/line\nbreak")) {
            assertFalse(url, SecureTransport.isHttpsUrl(url))
            assertThrows(InsecureTransportException::class.java) { SecureTransport.requireHttps(url) }
        }
    }

    @Test fun httpsRedirectsRetainRelativeAndSameProtocolTargets() {
        val origin = "https://provider.example/account/list"
        assertEquals("https://provider.example/guide.xml", SecureTransport.redirect(origin, "/guide.xml"))
        assertEquals("https://cdn.example/media", SecureTransport.redirect(origin, "//cdn.example/media"))
        assertEquals("https://cdn.example/media", SecureTransport.redirect(origin, "https://cdn.example/media"))
    }

    @Test fun secureRedirectsCannotDowngradeOrSwitchToNonHttpsSchemes() {
        for (target in listOf("http://provider.example/private?password=synthetic", "HTTP://cdn.example/media", "ftp://cdn.example/media", "file:///local/file")) {
            val error = assertThrows(InsecureTransportException::class.java) {
                SecureTransport.redirect("https://provider.example/private?username=synthetic", target)
            }
            assertEquals(SecureTransport.SECURE_REDIRECT_REQUIRED, error.message)
            assertFalse(error.message.orEmpty().contains(target))
            assertFalse(error.message.orEmpty().contains("synthetic"))
        }
    }

    @Test fun normalJVMHttpFixtureRedirectsAndHttpsUpgradesRemainSupported() {
        assertEquals("http://127.0.0.1/next", SecureTransport.redirect("http://127.0.0.1/list", "/next"))
        assertEquals("https://provider.example/list", SecureTransport.redirect("http://provider.example/list", "https://provider.example/list"))
    }

    @Test fun insecureProbeExplainsHttpsInsteadOfSuggestingNetworkOrVpnChanges() {
        val result = StreamDiagnosis.probe("http://127.0.0.1:9/private?password=synthetic", "Synthetic/1")
        assertEquals(StreamDiagnosis.Kind.SECURE_TRANSPORT_REQUIRED, result.kind)
        assertEquals(SecureTransport.HTTPS_REQUIRED, StreamDiagnosis.message(result, isArchive = false))
        assertFalse(StreamDiagnosis.message(result, isArchive = true).contains("VPN"))
        assertFalse(StreamDiagnosis.message(result, isArchive = true).contains("synthetic"))
    }

    @Test fun blockedRedirectDiagnosisRetainsOnlyTheSafeTransportExplanation() {
        val redirect = StreamDiagnosis.classify(0, null, byteArrayOf(),
            InsecureTransportException(SecureTransport.SECURE_REDIRECT_REQUIRED), "provider.example")
        assertEquals(StreamDiagnosis.Kind.SECURE_TRANSPORT_REQUIRED, redirect.kind)
        assertEquals(SecureTransport.SECURE_REDIRECT_REQUIRED, StreamDiagnosis.message(redirect, isArchive = false))
        val untrusted = StreamDiagnosis.classify(0, null, byteArrayOf(),
            InsecureTransportException("http://provider.example/private?password=synthetic"), "provider.example")
        assertEquals(SecureTransport.HTTPS_REQUIRED, StreamDiagnosis.message(untrusted, isArchive = false))
    }

    @Test fun everyRemoteMediaResourceRequiresHttpsInProduction() {
        assertTrue(SecureTransport.canLoadMedia("https://provider.example/segment.ts"))
        for (url in listOf("http://provider.example/private.ts", "http://127.0.0.1/demo.ts", "udp://provider.example:1234", "rtsp://provider.example/live", "rtmp://provider.example/live", "ftp://provider.example/file"))
            assertFalse(url, SecureTransport.canLoadMedia(url))
    }

    @Test fun localMediaSchemesAndLocalPathsKeepTheirExistingAccess() {
        for (url in listOf("file:///storage/My recording.ts", "content://media/external/video/1", "asset:///demo.ts", "android.resource://com.youriptv.app/123", "rawresource:///123", "data:video/mp2t;base64,AA==", "/storage/My recording.ts", "recordings/My recording.ts"))
            assertTrue(url, SecureTransport.canLoadMedia(url))
        assertFalse(SecureTransport.canLoadMedia("//provider.example/private.ts"))
        assertFalse(SecureTransport.canLoadMedia(""))
    }

    @Test fun isolatedQaHttpExceptionIsLiteralLoopbackOnly() {
        assertTrue(SecureTransport.canLoadMedia("http://127.0.0.1:18765/demo.ts", allowLoopbackHttp = true))
        for (url in listOf("http://provider.example/demo.ts", "http://127.0.0.1.evil.example/demo.ts", "http://127.0.0.1@evil.example/demo.ts", "http://evil@127.0.0.1/demo.ts", "http://%31%32%37.0.0.1/demo.ts", "http://2130706433/demo.ts", "http://127.1/demo.ts", "http://localhost/demo.ts", "http://127.0.0.1%2eevil.example/demo.ts"))
            assertFalse(url, SecureTransport.canLoadMedia(url, allowLoopbackHttp = true))
    }

    @Test fun externalPlayersReceiveHttpsOrLocalFilesAndContentOnly() {
        for (url in listOf("https://provider.example/live.ts", "file:///storage/My recording.ts", "content://media/external/video/1"))
            assertTrue(url, SecureTransport.canHandOffMedia(url))
        for (url in listOf("http://provider.example/private.ts", "http://127.0.0.1/demo.ts", "udp://provider.example:1234", "rtsp://provider.example/live", "rtmp://provider.example/live", "ftp://provider.example/file"))
            assertFalse(url, SecureTransport.canHandOffMedia(url))
        val nav = source("java/com/youriptv/app/ui/Nav.kt").substringAfter("fun externalPlayer(")
        assertTrue(nav.indexOf("SecureTransport.canHandOffMedia(url)") in 0 until nav.indexOf("Intent(Intent.ACTION_VIEW)"))
    }

    @Test fun productionNetworkPolicyForbidsCleartextWithoutDomainExceptions() {
        val builder = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
        val application = builder.parse(File(module, "src/main/AndroidManifest.xml")).getElementsByTagName("application").item(0) as org.w3c.dom.Element
        val android = "http://schemas.android.com/apk/res/android"
        assertEquals("false", application.getAttributeNS(android, "usesCleartextTraffic"))
        assertEquals("@xml/network_security_config", application.getAttributeNS(android, "networkSecurityConfig"))
        val policy = builder.parse(File(module, "src/main/res/xml/network_security_config.xml"))
        val base = policy.getElementsByTagName("base-config")
        assertEquals(1, base.length)
        assertEquals("false", (base.item(0) as org.w3c.dom.Element).getAttribute("cleartextTrafficPermitted"))
        assertEquals(0, policy.getElementsByTagName("domain-config").length)
        assertEquals(0, policy.getElementsByTagName("debug-overrides").length)
    }

    @Test fun isolatedNetworkPolicyHasNoRemoteCleartextException() {
        val policy = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(module, "src/isolatedTest/res/xml/network_security_config.xml"))
        val base = policy.getElementsByTagName("base-config").item(0) as org.w3c.dom.Element
        assertEquals("false", base.getAttribute("cleartextTrafficPermitted"))
        val domains = policy.getElementsByTagName("domain")
        assertEquals(1, domains.length)
        assertEquals("127.0.0.1", domains.item(0).textContent)
        assertEquals("false", (domains.item(0) as org.w3c.dom.Element).getAttribute("includeSubdomains"))
    }

    @Test fun sourceGuardsAreWiredBeforeNetworkAndPreserveTheSharedPlayer() {
        val editor = source("java/com/youriptv/app/ui/PlaylistEditActivity.kt").substringAfter("private fun savePlaylist(")
        val persistence = editor.indexOf("app.repo.savePlaylist(p)")
        assertTrue(editor.indexOf("!SecureTransport.isHttpsUrl(p.url)") in 0 until persistence)
        assertTrue(editor.indexOf("p.epgUrl.isNotBlank() && !SecureTransport.isHttpsUrl(p.epgUrl)") in 0 until persistence)
        assertFalse(editor.contains("Continue with HTTP"))
        val factory = source("java/com/youriptv/app/ui/PlaybackFactory.kt")
        assertTrue(factory.contains(".setAllowCrossProtocolRedirects(false)"))
        assertTrue(factory.contains("SecureDataSourceFactory(DefaultDataSource.Factory(appContext, http)"))
        assertTrue(factory.contains("BuildConfig.BUILD_TYPE == \"isolatedTest\""))
        val guarded = source("java/com/youriptv/app/ui/SecureDataSourceFactory.kt")
        assertTrue(guarded.contains("object : DataSource by source"))
        assertTrue(guarded.indexOf("SecureTransport.canLoadMedia(") in 0 until guarded.indexOf("return source.open(dataSpec)"))
        assertTrue(source("java/com/youriptv/app/data/Http.kt").contains("SecureTransport.redirect(current, loc)"))
        val diagnosis = source("java/com/youriptv/app/data/StreamDiagnosis.kt")
        assertTrue(diagnosis.indexOf("SecureTransport.requireHttps(url)") in 0 until diagnosis.indexOf(".openConnection()"))
        assertTrue(diagnosis.contains("SecureTransport.redirect(current, loc)"))
    }
}
