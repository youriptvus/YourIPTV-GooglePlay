package com.youriptv.app

import com.youriptv.app.ui.PlayerControlsVisibility
import org.junit.Assert.*
import org.junit.Test

class PlayerControlsVisibilityTest {
    private fun visible(live: Boolean, banner: Boolean = false, controller: Boolean = false,
                        television: Boolean = false, pip: Boolean = false, overlay: Boolean = false) =
        PlayerControlsVisibility.touchOptionsVisible(television, pip, live, banner, controller, overlay)

    @Test fun liveOptionsDisappearWithTimedBannerAndReturnOnTap() {
        assertTrue(visible(live = true, banner = true))
        assertFalse(visible(live = true, controller = true))
        assertTrue(visible(live = true, banner = true))
    }
    @Test fun moviesAndEpisodesFollowControllerAutoHideAndManualHide() {
        assertTrue(visible(live = false, controller = true))
        assertFalse(visible(live = false, banner = true))
        assertTrue(visible(live = false, controller = true))
    }
    @Test fun touchButtonCannotObscureTvPipOrChannelOverlay() {
        for (live in listOf(true, false)) {
            assertFalse(visible(live, banner = true, controller = true, television = true))
            assertFalse(visible(live, banner = true, controller = true, pip = true))
            assertFalse(visible(live, banner = true, controller = true, overlay = true))
        }
    }
}
