package com.youriptv.app

import com.youriptv.app.data.SettingsLockPolicy
import org.junit.Assert.*
import org.junit.Test

class SettingsLockPolicyTest {
    @Test fun freshPlayInstallationDefaultsOff() {
        assertFalse(SettingsLockPolicy.enabled(null))
        assertFalse(SettingsLockPolicy.initial(null, existingInstallation = false))
    }

    @Test fun oldUnclassifiedInstallationRemainsProtected() {
        assertTrue(SettingsLockPolicy.initial(null, existingInstallation = true))
    }

    @Test fun deliberateOnAndOffChoicesSurviveMigration() {
        listOf(false, true).forEach { existing ->
            assertTrue(SettingsLockPolicy.initial(true, existing))
            assertFalse(SettingsLockPolicy.initial(false, existing))
        }
    }

    @Test fun persistedChoiceSurvivesRestartAndObsoleteFlagsCannotOverrideIt() {
        val local = mutableMapOf(SettingsLockPolicy.LOCAL_KEY to SettingsLockPolicy.initial(null, false))
        fun restarted() = SettingsLockPolicy.enabled(local[SettingsLockPolicy.LOCAL_KEY])
        assertFalse(restarted())
        local["lock_settings"] = true
        assertFalse(restarted())
        local[SettingsLockPolicy.LOCAL_KEY] = true
        assertTrue(restarted())
        local[SettingsLockPolicy.LOCAL_KEY] = false
        assertFalse(restarted())
    }
}
