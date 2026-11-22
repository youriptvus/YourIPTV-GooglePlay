package com.youriptv.app.distribution

import android.app.Activity
import com.youriptv.app.YourIptvApp
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.youriptv.app.BuildConfig
import com.youriptv.app.data.Settings
import com.youriptv.app.ui.AppUpdateActivity
import com.youriptv.app.ui.Branding
import com.youriptv.app.ui.Dialogs
import com.youriptv.app.ui.YourIptvLogin
import com.youriptv.app.updates.AppUpdateChecks
import com.youriptv.app.updates.AppUpdates

/** Customer delivery retains the existing updater and the adding-only provider shortcut. */
internal object DistributionHooks {
    val playlistPreset: PlaylistPreset = object : PlaylistPreset {
        override val title = YourIptvLogin.PLAYLIST_NAME
        override fun newPlaylist(username: String, password: String) = YourIptvLogin.newPlaylist(username, password)
    }
    const val updatesDescription = "Automatic updates and installation permissions"

    fun onApplicationCreated(application: YourIptvApp) {
        application.registerActivityLifecycleCallbacks(AppUpdateChecks(application))
    }

    fun allowsRemoteBack(activity: Activity) = activity !is AppUpdateActivity

    fun updateEntries(activity: Activity, settings: Settings, canUse: () -> Boolean, refresh: () -> Unit) = listOf(
        DistributionUpdateEntry("Allow update installs (technician)", {
            if (AppUpdates.installationAuthorized(activity)) "Allowed · updates install without asking the customer"
            else "Not allowed · opens Android's “Install unknown apps” page for this app only"
        }, { if (canUse()) openInstallAuthorization(activity) }),
        DistributionUpdateEntry("Allow automatic restart after updates (technician)", {
            when {
                Build.VERSION.SDK_INT >= 35 -> "Not available on this Android version · customers reopen the app after an update"
                AppUpdates.restartsAutomatically(activity) -> "Allowed · the app reopens itself after an update"
                else -> "Not allowed · opens Android's “Display over other apps” page"
            }
        }, { if (canUse()) openRestartAuthorization(activity) }),
        DistributionUpdateEntry("Automatic updates", {
            if (settings.automaticAppUpdates) "On · checks every app opening · customer chooses Install or Ignore (required after 2 releases)"
            else "Off · version ${BuildConfig.VERSION_NAME}"
        }, {
            if (canUse()) {
                if (settings.automaticAppUpdates) {
                    settings.automaticAppUpdates = false
                    settings.lastUpdateInfo = null
                    refresh()
                } else {
                    settings.automaticAppUpdates = true
                    refresh()
                    Dialogs.message(activity, "Automatic updates enabled",
                        "Every time the app opens it checks the latest ${Branding.displayName(activity)} release on GitHub, using youriptv.us if GitHub is unavailable. Customers choose Install or Ignore; " +
                            "once the box is two releases behind, the update is required. " +
                            (if (!AppUpdates.installationAuthorized(activity)) "Android asks once to allow installs from this app unless “Allow update installs” is set here first."
                            else if (!AppUpdates.silentInstallSupported) "This Android version shows one Install confirmation for each update."
                            else if (AppUpdates.restartsAutomatically(activity)) "Updates install without permission prompts and the app restarts afterwards."
                            else "Updates install without permission prompts; the app then closes and the customer opens it again."))
                }
            }
        }),
    )

    private fun openRestartAuthorization(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 35) {
            Dialogs.message(activity, "Automatic restart", "Android 15 and newer do not let an app reopen itself after updating. Customers open the app again from the home screen.")
            return
        }
        if (AppUpdates.restartsAutomatically(activity)) {
            Dialogs.message(activity, "Automatic restart allowed", "The app reopens itself after an update.")
            return
        }
        try {
            activity.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}")))
        } catch (e: ActivityNotFoundException) {
            Dialogs.message(activity, "Technician setup",
                "This device has no “Display over other apps” screen. Without it, customers open the app again after an update.")
        }
    }

    private fun openInstallAuthorization(activity: Activity) {
        if (AppUpdates.installationAuthorized(activity)) {
            Dialogs.message(activity, "Update installs allowed", "This app is already allowed to install its own updates.")
            return
        }
        try {
            activity.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
        } catch (e: ActivityNotFoundException) {
            Dialogs.message(activity, "Technician setup",
                "This device has no “Install unknown apps” screen. Use Android Settings → Apps → Special app access → Install unknown apps → ${Branding.displayName(activity)} → Allow.")
        }
    }
}
