package com.youriptv.app

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Protects the shared layout contract used by both touch and remote navigation. */
class AdaptiveUiResourceTest {
    private val module = listOf(File("."), File("app")).first { File(it, "src/main/AndroidManifest.xml").exists() }
    private val resources = File(module, "src/main/res")
    private val android = "http://schemas.android.com/apk/res/android"

    private fun document(relative: String) = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File(resources, relative))

    private fun ids(relative: String): Set<String> {
        val nodes = document(relative).getElementsByTagName("*")
        return (0 until nodes.length).mapNotNull { index ->
            (nodes.item(index) as org.w3c.dom.Element).getAttributeNS(android, "id").takeIf { it.isNotEmpty() }
        }.toSet()
    }

    @Test fun portraitAndLandscapeKeepTheSharedBrowserControls() {
        val required = setOf("brand", "playlist_name", "clock", "nav_container", "groups", "items", "live_header", "live_timeline")
        for (layout in listOf("layout/activity_main.xml", "layout-port/activity_main.xml")) {
            val present = ids(layout).map { it.substringAfter('/') }.toSet()
            assertTrue("$layout is missing browser controls: ${required - present}", present.containsAll(required))
        }
    }

    @Test fun portraitPlayerKeepsAllDimensionsNeededByTheSharedPlayerLayout() {
        val nodes = document("values-port/player_dimensions.xml").getElementsByTagName("dimen")
        val overrides = (0 until nodes.length).associate { index ->
            val element = nodes.item(index) as org.w3c.dom.Element
            element.getAttribute("name") to element.textContent.trim().removeSuffix("dp").toInt()
        }
        assertTrue("Portrait banner must leave room for title and controls on a small phone",
            overrides.getValue("player_banner_logo_width") + overrides.getValue("player_banner_logo_margin") +
                2 * overrides.getValue("player_banner_padding_horizontal") <= 96)
        assertTrue("The guide timeline must fit beside programme times", overrides.getValue("player_banner_progress_width") <= 64)
        assertTrue("Movie artwork must leave space for portrait details", overrides.getValue("movie_cover_width") <= 100)
    }

    @Test fun touchPlaybackOptionsHaveAnAccessibleTouchTarget() {
        val nodes = document("layout/activity_player.xml").getElementsByTagName("Button")
        val button = (0 until nodes.length).map { nodes.item(it) as org.w3c.dom.Element }
            .single { it.getAttributeNS(android, "id") == "@+id/player_touch_options" }
        assertEquals("48dp", button.getAttributeNS(android, "minWidth"))
        assertEquals("48dp", button.getAttributeNS(android, "minHeight"))
        assertEquals("@string/player_touch_options", button.getAttributeNS(android, "text"))
    }
}
