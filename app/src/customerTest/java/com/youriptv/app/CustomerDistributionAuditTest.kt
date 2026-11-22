package com.youriptv.app

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CustomerDistributionAuditTest {
    private val module = listOf(File("."), File("app")).first { File(it, "src/main/AndroidManifest.xml").exists() }

    @Test fun customerOverlayContainsOnlyTheExistingUpdaterPermissionsAndPrivateActivity() {
        assertTrue(BuildConfig.IS_CUSTOMER_DISTRIBUTION)
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
            .parse(File(module, "src/customer/AndroidManifest.xml"))
        val android = "http://schemas.android.com/apk/res/android"
        val nodes = doc.getElementsByTagName("uses-permission")
        assertEquals(setOf("android.permission.REQUEST_INSTALL_PACKAGES", "android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION",
            "android.permission.SYSTEM_ALERT_WINDOW"), (0 until nodes.length).map {
            (nodes.item(it) as org.w3c.dom.Element).getAttributeNS(android, "name")
        }.toSet())
        val activity = doc.getElementsByTagName("activity").item(0) as org.w3c.dom.Element
        assertEquals(".ui.AppUpdateActivity", activity.getAttributeNS(android, "name"))
        assertEquals("false", activity.getAttributeNS(android, "exported"))
    }

    @Test fun officialUpdaterAddressesRemainCompatibleWithPublishedCustomerClients() {
        assertEquals("https://github.com/youriptvus/YourIPTV/releases/latest/download/update.json", BuildConfig.UPDATE_MANIFEST_URL)
        assertEquals("https://youriptv.us/updates/update.json", BuildConfig.UPDATE_BACKUP_MANIFEST_URL)
    }
}
