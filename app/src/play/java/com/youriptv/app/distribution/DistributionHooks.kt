package com.youriptv.app.distribution

import android.app.Activity
import com.youriptv.app.YourIptvApp
import com.youriptv.app.BuildConfig
import com.youriptv.app.data.Settings
import com.youriptv.app.ui.Dialogs

/** Google Play delivery has no customer updater, installation permission, or provider preset. */
internal object DistributionHooks {
    val playlistPreset: PlaylistPreset? = null
    const val updatesDescription = "Updates delivered by Google Play"

    fun onApplicationCreated(application: YourIptvApp) = Unit
    fun allowsRemoteBack(activity: Activity) = true

    fun updateEntries(activity: Activity, settings: Settings, canUse: () -> Boolean, refresh: () -> Unit) = listOf(
        DistributionUpdateEntry("Google Play updates", {
            "Version ${BuildConfig.VERSION_NAME} · updates are delivered by Google Play"
        }, {
            if (canUse()) Dialogs.message(activity, "Google Play updates",
                "Manage automatic updates in Google Play on this device. Playback works without a Play Store connection. YourIPTV does not download or install APK updates.")
        }),
    )
}
