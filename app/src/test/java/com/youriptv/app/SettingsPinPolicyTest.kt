package com.youriptv.app

import com.youriptv.app.data.PinCheck
import com.youriptv.app.data.PinKind
import com.youriptv.app.data.PinVault
import com.youriptv.app.data.StringStore
import org.junit.Assert.*
import org.junit.Test

/** Executable policy tests; these use no Android preferences, dialogs or real installation data. */
class SettingsPinPolicyTest {
    private class MapStore : StringStore {
        val values = mutableMapOf<String, String>()
        override fun get(key: String) = values[key]
        override fun put(values: Map<String, String?>) {
            values.forEach { (key, value) -> if (value == null) this.values.remove(key) else this.values[key] = value }
        }
    }

    private val store = MapStore()
    private var now = 1_000_000L
    private val vault = PinVault(store, customerDistribution = false) { now }
    private val master = "846275" // Synthetic local Admin PIN used only by tests.
    private val custom = "13579"
    private val parental = "2468"

    init { vault.setAdmin(master) }

    @Test
    fun withoutCustomPinSettingsStillRequireMaster() {
        assertFalse(vault.has(PinKind.SETTINGS))
        assertEquals(PinCheck.Wrong(4), vault.checkSettingsAccess("0000"))
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess(master))
        assertFalse("Admin access must not create a custom PIN", vault.has(PinKind.SETTINGS))
        assertFalse("The local Admin PIN is never stored in plaintext", store.values.values.contains(master))
        assertTrue(vault.hasAdmin())
    }

    @Test
    fun existingCustomAndMasterBothOpenSettingsWithoutReplacingStoredPin() {
        vault.set(PinKind.SETTINGS, custom)
        val storedPin = store.values.toMap()
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess(custom))
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess(master))
        assertEquals(storedPin, store.values)
    }

    @Test
    fun customSettingsAccessDoesNotAuthorizeCustomPinAdministration() {
        vault.set(PinKind.SETTINGS, custom)
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess(custom))
        assertEquals(PinCheck.Wrong(4), vault.checkSettingsMaster(custom))
        assertEquals(PinCheck.Ok, vault.checkSettingsMaster(master))
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess(custom))
    }

    @Test
    fun wrongSubmissionCountsOnceAcrossOrdinaryAndMasterOnlyPrompts() {
        vault.set(PinKind.SETTINGS, custom)
        assertEquals(PinCheck.Wrong(4), vault.checkSettingsAccess("0000"))
        assertEquals(1, vault.failures(PinKind.SETTINGS))
        assertEquals(PinCheck.Wrong(3), vault.checkSettingsMaster(custom))
        assertEquals(2, vault.failures(PinKind.SETTINGS))
        assertEquals(PinCheck.Wrong(2), vault.checkSettingsAccess("1111"))
        assertEquals(PinCheck.Wrong(1), vault.checkSettingsMaster("2222"))
        assertTrue(vault.checkSettingsAccess("3333") is PinCheck.LockedOut)
        assertEquals(5, vault.failures(PinKind.SETTINGS))
        assertTrue(vault.checkSettingsAccess(master) is PinCheck.LockedOut)
        assertTrue(vault.checkSettingsAccess(custom) is PinCheck.LockedOut)
        assertTrue(vault.checkSettingsMaster(master) is PinCheck.LockedOut)
        assertEquals("Refused attempts during lockout do not add failures", 5, vault.failures(PinKind.SETTINGS))
    }

    @Test
    fun noCustomPinDoesNotBypassSharedSettingsLockout() {
        repeat(PinVault.MAX_ATTEMPTS) { vault.checkSettingsMaster("0000") }
        assertFalse(vault.has(PinKind.SETTINGS))
        assertTrue(vault.checkSettingsAccess(master) is PinCheck.LockedOut)
        assertTrue(vault.checkSettingsMaster(master) is PinCheck.LockedOut)
        now += PinVault.lockoutMs(PinVault.MAX_ATTEMPTS)
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess(master))
        assertEquals(0, vault.failures(PinKind.SETTINGS))
    }

    @Test
    fun preexistingCustomPinLockoutAlsoRestrictsMaster() {
        vault.set(PinKind.SETTINGS, custom)
        repeat(PinVault.MAX_ATTEMPTS) { vault.check(PinKind.SETTINGS, "0000") }
        val previous = store.values.toMap()
        assertTrue(vault.checkSettingsAccess(master) is PinCheck.LockedOut)
        assertTrue(vault.checkSettingsMaster(master) is PinCheck.LockedOut)
        assertEquals(previous, store.values)
    }

    @Test
    fun sharedLockoutSurvivesRestartAndGrowsUntilSuccessfulAuthentication() {
        repeat(PinVault.MAX_ATTEMPTS) { vault.checkSettingsAccess("0000") }
        val restarted = PinVault(store, customerDistribution = false) { now }
        assertTrue(restarted.checkSettingsMaster(master) is PinCheck.LockedOut)
        now += PinVault.lockoutMs(PinVault.MAX_ATTEMPTS)
        val next = restarted.checkSettingsMaster("0000")
        assertTrue(next is PinCheck.LockedOut)
        assertEquals(PinVault.lockoutMs(6), (next as PinCheck.LockedOut).remainingMs)
        assertTrue(restarted.checkSettingsAccess(master) is PinCheck.LockedOut)
        now += PinVault.lockoutMs(6)
        assertEquals(PinCheck.Ok, restarted.checkSettingsMaster(master))
        assertEquals(0, restarted.failures(PinKind.SETTINGS))
        assertEquals(0L, restarted.lockoutRemaining(PinKind.SETTINGS))
    }

    @Test
    fun successResetsOnlySettingsCountersAndKeepsParentalState() {
        vault.set(PinKind.PARENTAL, parental)
        vault.set(PinKind.SETTINGS, custom)
        repeat(PinVault.MAX_ATTEMPTS) { vault.check(PinKind.PARENTAL, "0000") }
        vault.checkSettingsMaster("0000")
        val parentalState = store.values.filterKeys { it.startsWith("pin_") }
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess(custom))
        assertEquals(0, vault.failures(PinKind.SETTINGS))
        assertEquals(parentalState, store.values.filterKeys { it.startsWith("pin_") })
        vault.checkSettingsAccess("0000")
        assertEquals(PinCheck.Ok, vault.checkSettingsMaster(master))
        assertEquals(0, vault.failures(PinKind.SETTINGS))
        assertEquals(parentalState, store.values.filterKeys { it.startsWith("pin_") })
        assertTrue(vault.check(PinKind.PARENTAL, parental) is PinCheck.LockedOut)
    }

    @Test
    fun settingsMasterDoesNotBecomeAParentalOverride() {
        vault.set(PinKind.PARENTAL, parental)
        assertEquals(PinCheck.Wrong(4), vault.check(PinKind.PARENTAL, master))
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess(master))
        assertEquals("Settings master must not reset parental failures", 1, vault.failures(PinKind.PARENTAL))
        assertEquals(PinCheck.Ok, vault.check(PinKind.PARENTAL, parental))
    }

    @Test
    fun existingParentalPinEqualToMasterIsNotRewrittenOrGivenNewRestrictions() {
        vault.set(PinKind.PARENTAL, master)
        vault.set(PinKind.SETTINGS, custom)
        vault.check(PinKind.PARENTAL, "0000")
        val parentalState = store.values.filterKeys { it.startsWith("pin_") }
        assertEquals(PinCheck.Ok, vault.checkSettingsMaster(master))
        assertEquals(parentalState, store.values.filterKeys { it.startsWith("pin_") })
        assertEquals(PinCheck.Ok, vault.check(PinKind.PARENTAL, master))
    }

    @Test
    fun changingAndRemovingCustomPinNeverRemovesAdminAccess() {
        vault.set(PinKind.SETTINGS, custom)
        assertEquals(PinCheck.Ok, vault.checkSettingsMaster(master))
        vault.set(PinKind.SETTINGS, "97531")
        assertTrue(vault.checkSettingsAccess(custom) is PinCheck.Wrong)
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess("97531"))
        assertEquals(PinCheck.Ok, vault.checkSettingsMaster(master))
        vault.set(PinKind.SETTINGS, null)
        assertFalse(vault.has(PinKind.SETTINGS))
        assertTrue(vault.checkSettingsAccess("97531") is PinCheck.Wrong)
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess(master))
        assertEquals(PinCheck.Ok, vault.checkSettingsMaster(master))
        assertTrue(vault.hasAdmin())
        assertFalse(store.values.values.contains(master))
    }

    @Test
    fun malformedSettingsEntriesNeverCountAsAdminAccess() {
        for (entry in listOf("", "8", "846", "0846275", "8462750", " 846275", "846275 ", "84a275")) {
            assertTrue("Malformed or nonmatching input must be rejected", vault.checkSettingsAccess(entry) is PinCheck.Wrong)
            assertEquals(PinCheck.Ok, vault.checkSettingsMaster(master))
        }
    }

    @Test
    fun parentAndCustomPinPrimitiveSemanticsRemainUnchanged() {
        assertEquals(PinCheck.Ok, vault.check(PinKind.PARENTAL, "anything"))
        assertEquals(PinCheck.Ok, vault.check(PinKind.SETTINGS, "anything"))
        vault.set(PinKind.PARENTAL, parental)
        assertNotNull(vault.validateNew(PinKind.SETTINGS, parental))
        assertTrue(runCatching { vault.set(PinKind.SETTINGS, parental) }.isFailure)
        assertFalse(vault.has(PinKind.SETTINGS))
        assertEquals(PinCheck.Ok, vault.checkSettingsAccess(master))
        assertEquals(PinCheck.Ok, vault.check(PinKind.PARENTAL, parental))
    }

    @Test fun freshVaultRequiresDeliberateAdminSetupAndHasNoSharedDefault() {
        val fresh = PinVault(MapStore(), customerDistribution = false)
        assertTrue(fresh.needsAdminSetup())
        assertFalse(fresh.hasAdmin())
        assertFalse(fresh.legacyAdminAllowed())
        assertTrue(fresh.checkSettingsMaster("123456") is PinCheck.Wrong)
        assertTrue(fresh.checkSettingsAccess("123456") is PinCheck.Wrong)
    }

    @Test fun migratedCustomSettingsPinCannotEnrollOrBecomeAdmin() {
        val migratedStore = MapStore()
        migratedStore.put(mapOf(PinVault.LEGACY_ADMIN_KEY to "true"))
        val migrated = PinVault(migratedStore, customerDistribution = false) { now }
        migrated.set(PinKind.SETTINGS, custom)
        assertFalse(migrated.needsAdminSetup())
        assertTrue(migrated.legacyAdminAllowed())
        assertEquals(PinCheck.Ok, migrated.checkSettingsAccess(custom))
        assertTrue(migrated.checkSettingsMaster(custom) is PinCheck.Wrong)
        assertFalse(migrated.hasAdmin())
    }

    @Test fun localAdminEnrollmentDisablesLegacyVerifierAndSurvivesRestart() {
        val migratedStore = MapStore()
        migratedStore.put(mapOf(PinVault.LEGACY_ADMIN_KEY to "true"))
        val migrated = PinVault(migratedStore, customerDistribution = false) { now }
        migrated.setAdmin(master)
        assertFalse(migratedStore.values.containsKey(PinVault.LEGACY_ADMIN_KEY))
        assertFalse(migratedStore.values.values.contains(master))
        val restarted = PinVault(migratedStore, customerDistribution = false) { now }
        assertFalse(restarted.legacyAdminAllowed())
        assertEquals(PinCheck.Ok, restarted.checkSettingsMaster(master))
        assertNotNull(restarted.validateAdmin("12345"))
        assertNotNull(restarted.validateAdmin("123456789"))
        assertNotNull(restarted.validateAdmin("abcdef"))
        assertTrue(runCatching { restarted.setAdmin("1234") }.isFailure)
    }
}
