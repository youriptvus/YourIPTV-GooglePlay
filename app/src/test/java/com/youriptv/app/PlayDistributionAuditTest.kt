package com.youriptv.app

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Shipping boundaries: inspect the actual production declaration and executable source set. */
class PlayDistributionAuditTest {
    private val module = listOf(File("."), File("app")).first { File(it, "src/main/AndroidManifest.xml").exists() }
    private val main = File(module, "src/main")
    private val android = "http://schemas.android.com/apk/res/android"
    private fun manifest() = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File(main, "AndroidManifest.xml"))

    @Test fun installerAndOverlayPermissionsAreAbsent() {
        val nodes = manifest().getElementsByTagName("uses-permission")
        val permissions = (0 until nodes.length).map {
            (nodes.item(it) as org.w3c.dom.Element).getAttributeNS(android, "name")
        }.toSet()
        assertEquals(setOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"), permissions)
        val code = File(main, "java").walkTopDown().filter { it.extension in setOf("kt", "java") }.map { it.readText() }.joinToString("\n")
        for (installer in listOf("PackageInstaller", "ACTION_INSTALL_PACKAGE", "ACTION_MANAGE_UNKNOWN_APP_SOURCES",
            "application/vnd.android.package-archive", "canRequestPackageInstalls", "ACTION_MANAGE_OVERLAY_PERMISSION")) {
            assertFalse("Self-installation path: $installer", code.contains(installer))
        }
    }

    @Test fun productionHasOneIdentityAndBothLaunchersWithoutRequiredTouchscreen() {
        val build = File(module, "build.gradle.kts").readText()
        assertEquals(1, Regex("applicationId = ").findAll(build).count())
        assertTrue(build.contains("applicationId = \"com.youriptv.app\""))
        assertTrue(build.contains("versionCode = 50"))
        assertTrue(build.contains("versionName = \"1.0.9\""))
        assertTrue("Enabled Google Play automatic protection requires minimum API 24", build.contains("minSdk = 24"))
        assertTrue(build.contains("targetSdk = 36"))
        assertFalse(build.contains("productFlavors"))
        val doc = manifest()
        val activities = doc.getElementsByTagName("activity")
        val entry = (0 until activities.length).map { activities.item(it) as org.w3c.dom.Element }
            .single { it.getAttributeNS(android, "exported") == "true" }
        assertEquals(".ui.MainActivity", entry.getAttributeNS(android, "name"))
        val categories = entry.getElementsByTagName("category")
        assertEquals(setOf("android.intent.category.LAUNCHER", "android.intent.category.LEANBACK_LAUNCHER"),
            (0 until categories.length).map { (categories.item(it) as org.w3c.dom.Element).getAttributeNS(android, "name") }.toSet())
        val features = doc.getElementsByTagName("uses-feature")
        for (i in 0 until features.length) assertEquals("false", (features.item(i) as org.w3c.dom.Element).getAttributeNS(android, "required"))
    }

    @Test fun privacyIsBundledAndPubliclyReachableWithoutNetworkOrPin() {
        val ui = File(main, "java/com/youriptv/app/ui")
        assertTrue(File(ui, "AboutActivity.kt").readText().contains("PrivacyActivity::class.java"))
        val privacy = File(ui, "PrivacyActivity.kt").readText()
        assertFalse(privacy.contains("requirePin") || privacy.contains("Http.") || privacy.contains("ACTION_VIEW"))
        assertTrue(File(main, "res/values/privacy.xml").exists())
        assertEquals("false", (manifest().getElementsByTagName("application").item(0) as org.w3c.dom.Element)
            .getAttributeNS(android, "allowBackup"))
    }

    @Test fun signingMaterialCannotBeLoadedFromTheRepository() {
        val build = File(module, "build.gradle.kts").readText()
        assertTrue(build.contains("YOURIPTV_UPLOAD_SIGNING_PROPERTIES"))
        assertEquals(2, Regex("startsWith\\(rootDir.canonicalFile.toPath\\(\\)\\)").findAll(build).count())
        assertFalse(build.contains("rootProject.file(\"keystore.properties\")"))
        assertFalse(build.contains("UPDATE_MANIFEST_URL"))
    }

    @Test fun automaticBackupsAndDeviceTransfersExcludeCredentialsAndLocalSecurity() {
        val application = manifest().getElementsByTagName("application").item(0) as org.w3c.dom.Element
        assertEquals("false", application.getAttributeNS(android, "allowBackup"))
        assertEquals("@xml/backup_rules", application.getAttributeNS(android, "fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.getAttributeNS(android, "dataExtractionRules"))
        assertFalse("A custom backup agent must not bypass the declared exclusion boundary",
            application.hasAttributeNS(android, "backupAgent"))
        val credentialDomains = setOf("root", "file", "database", "sharedpref", "external")
        val allDomains = credentialDomains + setOf("device_root", "device_file", "device_database", "device_sharedpref")
        fun document(path: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(main, path))
        fun requireCompleteExclusion(element: org.w3c.dom.Element, expected: Set<String>) {
            assertEquals("Automatic transfers must not reinclude credentials or local policy", 0,
                element.getElementsByTagName("include").length)
            val excludes = element.getElementsByTagName("exclude")
            assertEquals(expected.size, excludes.length)
            val domains = (0 until excludes.length).map {
                val exclude = excludes.item(it) as org.w3c.dom.Element
                assertEquals("Every exclusion covers the complete domain recursively", ".", exclude.getAttribute("path"))
                exclude.getAttribute("domain")
            }.toSet()
            assertEquals(expected, domains)
        }
        val legacy = document("res/xml/backup_rules.xml").documentElement
        assertEquals("full-backup-content", legacy.tagName)
        requireCompleteExclusion(legacy, credentialDomains)
        val directBoot = document("res/xml-v24/backup_rules.xml").documentElement
        assertEquals("full-backup-content", directBoot.tagName)
        requireCompleteExclusion(directBoot, allDomains)
        val modern = document("res/xml/data_extraction_rules.xml").documentElement
        assertEquals("data-extraction-rules", modern.tagName)
        for (mode in listOf("cloud-backup", "device-transfer")) {
            val sections = modern.getElementsByTagName(mode)
            assertEquals("A missing transfer mode would enable its default extraction", 1, sections.length)
            requireCompleteExclusion(sections.item(0) as org.w3c.dom.Element, allDomains)
        }
    }

    @Test fun updateNavigationDescribesThePlayChannel() {
        val category = com.youriptv.app.ui.SettingsCategory.UPDATES
        assertTrue(category.description.contains("Google Play"))
        assertFalse(category.description.contains("GitHub"))
    }
}
