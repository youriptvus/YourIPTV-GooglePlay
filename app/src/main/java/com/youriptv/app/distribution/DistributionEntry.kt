package com.youriptv.app.distribution

import com.youriptv.app.data.Playlist

/** Small UI contracts; installer and provider implementations live only in the customer source set. */
internal class DistributionUpdateEntry(
    val title: String,
    val value: () -> String,
    val action: () -> Unit,
)

internal interface PlaylistPreset {
    val title: String
    fun newPlaylist(username: String, password: String): Playlist?
}
