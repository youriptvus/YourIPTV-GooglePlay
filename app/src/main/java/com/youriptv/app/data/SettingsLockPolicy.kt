package com.youriptv.app.data

/** An explicit local choice; neither a custom PIN nor the obsolete shared-lock flag controls it. */
object SettingsLockPolicy {
    const val LOCAL_KEY = "settings_lock_enabled"

    const val PLAY_INITIALIZED_KEY = "play_security_initialized"

    /** Missing state on a fresh Play installation means ordinary Settings are accessible. */
    fun enabled(saved: Boolean?): Boolean = saved ?: false

    /** Existing unclassified installations remain protected; no intentional choice is inferred. */
    fun initial(saved: Boolean?, existingInstallation: Boolean): Boolean = saved ?: existingInstallation
}
