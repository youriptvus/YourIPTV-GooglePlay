package com.youriptv.app.ui

/** Touch options belong to the transient controls, never the video surface itself. */
internal object PlayerControlsVisibility {
    fun touchOptionsVisible(
        television: Boolean, pictureInPicture: Boolean, live: Boolean,
        bannerVisible: Boolean, controllerVisible: Boolean, channelOverlayVisible: Boolean,
    ): Boolean = !television && !pictureInPicture && !channelOverlayVisible &&
        if (live) bannerVisible else controllerVisible
}
