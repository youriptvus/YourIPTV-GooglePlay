package com.youriptv.app

import com.youriptv.app.data.PinCheck
import com.youriptv.app.data.PinVault
import com.youriptv.app.data.StringStore
import com.youriptv.app.distribution.DistributionHooks
import org.junit.Assert.*
import org.junit.Test

/** Exercise the selected compiled delivery boundary, including the two independent Settings policies. */
class DistributionBoundaryTest {
    private class Store : StringStore {
        val values = mutableMapOf<String, String>()
        override fun get(key: String) = values[key]
        override fun put(values: Map<String, String?>) {
            values.forEach { (key, value) -> if (value == null) this.values.remove(key) else this.values[key] = value }
        }
    }

    @Test fun onlyCustomerCompilationContainsUpdaterAndProviderImplementations() {
        val loader = javaClass.classLoader
        for (name in listOf("updates.AppUpdates", "updates.UpdateTransport", "ui.AppUpdateActivity", "ui.YourIptvLogin")) {
            val present = runCatching { Class.forName("com.youriptv.app.$name", false, loader) }.isSuccess
            assertEquals("Unexpected executable delivery code: $name", BuildConfig.IS_CUSTOMER_DISTRIBUTION, present)
        }
        val updateFields = BuildConfig::class.java.declaredFields.map { it.name }.filter { it.startsWith("UPDATE_") }.toSet()
        assertEquals(if (BuildConfig.IS_CUSTOMER_DISTRIBUTION) setOf("UPDATE_MANIFEST_URL", "UPDATE_BACKUP_MANIFEST_URL",
            "UPDATE_TEST_ORIGIN", "UPDATE_TEST_BACKUP_ORIGIN") else emptySet<String>(), updateFields)
        assertEquals(BuildConfig.IS_CUSTOMER_DISTRIBUTION, DistributionHooks.playlistPreset != null)
    }

    @Test fun freshDefaultVaultUsesTheSelectedDistributionWithoutChangingOrdinaryPinStorage() {
        val vault = PinVault(Store())
        assertEquals(!BuildConfig.IS_CUSTOMER_DISTRIBUTION, vault.needsAdminSetup())
        assertEquals(BuildConfig.IS_CUSTOMER_DISTRIBUTION, vault.legacyAdminAllowed())
        assertFalse(vault.hasAdmin())
    }

    @Test fun customerMasterPolicyDoesNotAdoptAnUnrelatedPlayAdminCredential() {
        val store = Store()
        val play = PinVault(store, customerDistribution = false)
        play.setAdmin("846275")
        assertEquals(PinCheck.Ok, play.checkSettingsMaster("846275"))
        val customer = PinVault(store, customerDistribution = true)
        assertFalse(customer.needsAdminSetup())
        assertTrue(customer.legacyAdminAllowed())
        assertTrue(customer.checkSettingsMaster("846275") is PinCheck.Wrong)
        assertEquals(PinCheck.Ok, play.checkSettingsMaster("846275"))
    }

    @Test fun freshPlayDoesNotInheritCustomerMasterAuthorityWithoutUpgradeProvenance() {
        val store = Store()
        val play = PinVault(store, customerDistribution = false)
        val customer = PinVault(store, customerDistribution = true)
        assertFalse(play.legacyAdminAllowed())
        assertTrue(play.needsAdminSetup())
        assertTrue(customer.legacyAdminAllowed())
        assertFalse(customer.needsAdminSetup())
        assertTrue(store.values.isEmpty())
    }
}
