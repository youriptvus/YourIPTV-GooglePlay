package com.youriptv.app

import android.app.Activity
import android.app.Instrumentation
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.youriptv.app.data.*
import com.youriptv.app.ui.*
import org.json.JSONArray
import org.json.JSONObject

/** Physical remote-key and guide navigation checks, generated fixtures in the isolated package only. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class NavigationUiInstrumentation : Instrumentation() {
    private val opened = ArrayList<Activity>()
    private val results = JSONArray()
    private var failed = 0
    private fun main(action: () -> Unit) {
        var error: Throwable? = null
        runOnMainSync { try { action() } catch (failure: Throwable) { error = failure } }
        error?.let { throw it }
    }
    private fun await(timeout: Long = 15_000, condition: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < end) {
            var pass = false
            main { pass = condition() }
            if (pass) return
            SystemClock.sleep(100)
        }
        error("Navigation UI condition timed out")
    }
    private fun verify(name: String, action: () -> Unit) {
        try { action(); results.put(JSONObject().put("name", name).put("passed", true)) }
        catch (error: Throwable) {
            failed++
            results.put(JSONObject().put("name", name).put("passed", false).put("type", error.javaClass.simpleName)
                .put("message", error.message.orEmpty().take(1600)).put("stack", error.stackTrace.take(8).joinToString("\n")))
        }
    }
    private fun capture(name: String) {
        waitForIdleSync(); SystemClock.sleep(400); waitForIdleSync()
        val image = uiAutomation.takeScreenshot() ?: return
        val folder = java.io.File(targetContext.getExternalFilesDir(null), "navigation-qa").apply { mkdirs() }
        java.io.File(folder, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
    private fun finishScreen(screen: Activity) {
        main { if (!screen.isDestroyed) screen.finish() }
        waitForIdleSync(); SystemClock.sleep(350)
    }
    private fun nav(screen: Activity, label: String) = main {
        val container = screen.findViewById<LinearLayout>(R.id.nav_container)
        (0 until container.childCount).map { container.getChildAt(it) }.filterIsInstance<TextView>()
            .single { it.text.toString() == label }.performClick()
    }
    private fun heldBack() {
        val down = SystemClock.uptimeMillis()
        sendKeySync(KeyEvent(down, down, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0))
        SystemClock.sleep(850)
        sendKeySync(KeyEvent(down, SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 1,
            0, android.view.KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_LONG_PRESS))
        SystemClock.sleep(100)
        sendKeySync(KeyEvent(down, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0))
    }
    private fun grid(screen: Activity): LiveGuideGrid = MainActivity::class.java.getDeclaredField("liveGrid").let {
        it.isAccessible = true; it.get(screen) as LiveGuideGrid
    }
    private fun state(screen: Activity): Any = LiveGuideGrid::class.java.getDeclaredField("state").let {
        it.isAccessible = true; it.get(grid(screen))
    }
    private fun time(screen: Activity, field: String): Long = state(screen).let { value ->
        value.javaClass.getDeclaredField(field).let { it.isAccessible = true; it.getLong(value) }
    }
    private fun openGuide(screen: Activity) {
        nav(screen, "TV")
        val groups = screen.findViewById<RecyclerView>(R.id.groups)
        await { (groups.adapter?.itemCount ?: 0) >= 2 }
        main { groups.scrollToPosition(1) }
        await { groups.findViewHolderForAdapterPosition(1) != null }
        main { groups.findViewHolderForAdapterPosition(1)!!.itemView.performClick() }
        val rows = screen.findViewById<RecyclerView>(R.id.items)
        await { grid(screen).items.size == 1 && rows.adapter === grid(screen).adapter &&
            screen.findViewById<View>(R.id.state_container).visibility != View.VISIBLE }
        await { rows.findViewHolderForAdapterPosition(0) != null }
        main { rows.findViewHolderForAdapterPosition(0)!!.itemView.requestFocus() }
        await { rows.hasFocus() }
    }
    private fun assertNow(screen: Activity) {
        await { kotlin.math.abs(time(screen, "focusTime") - System.currentTimeMillis()) < 10_000 && grid(screen).isAtNow() }
        main {
            val now = System.currentTimeMillis()
            val start = time(screen, "windowStart")
            val span = time(screen, "span")
            check(start <= now && now < start + span)
            val channel = grid(screen).items.single()
            val focused = LiveGuideGrid::class.java.getDeclaredMethod("focusedProgram", MediaItem::class.java).let {
                it.isAccessible = true; it.invoke(grid(screen), channel) as? Program
            }
            check(focused != null && focused.start <= now && now < focused.end)
        }
    }
    private fun openSettings(screen: Activity): Activity {
        val monitor = addMonitor(SettingsActivity::class.java.name, null, false)
        return try {
            nav(screen, "Settings")
            (waitForMonitorWithTimeout(monitor, 10_000) ?: error("Settings did not open")).also {
                opened.add(it); await { it.hasWindowFocus() }
            }
        } finally { removeMonitor(monitor) }
    }
    private fun holdToPlayer(live: MediaItem, captureName: String): Activity {
        val monitor = addMonitor(PlayerActivity::class.java.name, null, false)
        return try {
            heldBack()
            val screen = waitForMonitorWithTimeout(monitor, 15_000) ?: error("Held BACK did not open current fullscreen stream")
            opened.add(screen)
            await { screen.hasWindowFocus() && screen.findViewById<PlayerView>(R.id.player_view).player?.let {
                it.playbackState == Player.STATE_READY && it.currentMediaItem?.mediaId == live.url } == true }
            main { screen.findViewById<PlayerView>(R.id.player_view).player!!.pause() }
            SystemClock.sleep(500)
            main { check(!screen.isFinishing && !screen.isDestroyed && screen.hasWindowFocus()) }
            capture(captureName)
            screen
        } finally { removeMonitor(monitor) }
    }
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        if (targetContext.packageName != "com.youriptv.app.playtest") {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("error", "Isolated package required") }); return
        }
        waitForIdleSync()
        val app = YourIptvApp.instance
        val preferences = targetContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val keys = listOf("active_playlist", "last_channel", "last_live_playlist", "resume_last", "epg_shift_h", "epg_past_h", "epg_future_d", SettingsLockPolicy.LOCAL_KEY)
        val original = keys.associateWith { preferences.all[it] }
        val oldQueue = YourIptvApp.playQueue
        var playlistId = 0L
        var browsingId = 0L
        try {
            val now = System.currentTimeMillis()
            playlistId = app.repo.savePlaylist(Playlist(name = "Navigation UI QA ${System.nanoTime()}", type = SourceType.M3U,
                url = "http://127.0.0.1:18766/navigation-list.m3u", refreshHours = 0, lastUpdated = now, liveCount = 1))
            val live = MediaItem(playlistId = playlistId, kind = ItemKind.LIVE, name = "Navigation live", group = "Navigation QA",
                url = "http://127.0.0.1:18766/navigation-live.mp4", tvgId = "navigation-fixture", catchupDays = 2)
            browsingId = app.repo.savePlaylist(Playlist(name = "Navigation UI QA other source ${System.nanoTime()}", type = SourceType.M3U,
                url = "http://127.0.0.1:18766/navigation-other.m3u", refreshHours = 0, lastUpdated = now, liveCount = 1))
            Db(targetContext).use { helper ->
                helper.writableDatabase.insertOrThrow("items", null, Db.itemValues(live))
                helper.writableDatabase.insertOrThrow("items", null, Db.itemValues(live.copy(playlistId = browsingId,
                    name = "Wrong source channel", group = "Other navigation QA", tvgId = "other-navigation-fixture")))
                val hour = now / 3_600_000 * 3_600_000
                for (offset in -8..8) helper.writableDatabase.insertOrThrow("programs", null, ContentValues().apply {
                    put("playlist_id", playlistId); put("channel_id", live.tvgId); put("start", hour + offset * 3_600_000L)
                    put("stop", hour + (offset + 1) * 3_600_000L); put("title", "Navigation programme $offset"); put("descr", "Generated guide fixture")
                })
            }
            app.settings.activePlaylistId = browsingId
            app.settings.rememberLiveChannel(live)
            app.settings.resumeLastChannel = true
            app.settings.epgShiftHours = 0
            app.settings.epgPastHours = 168
            app.settings.epgFutureDays = 2
            val coldMonitor = addMonitor(PlayerActivity::class.java.name, null, false)
            val home = startActivitySync(Intent(targetContext, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(MainActivity.EXTRA_GUIDE_CHANNEL, live.url)
                .putExtra(MainActivity.EXTRA_GUIDE_GROUP, live.group).putExtra(MainActivity.EXTRA_GUIDE_PLAYLIST, playlistId))
            opened.add(home)
            await { home.hasWindowFocus() }
            verify("cold guide request selects the requested source and channel at now instead of the browsing source or fullscreen") {
                try {
                    await { grid(home).items.any { it.playlistId == playlistId && it.url == live.url } &&
                        app.settings.activePlaylistId == playlistId }
                    SystemClock.sleep(1000)
                    main {
                        check(app.settings.activePlaylistId == playlistId &&
                            grid(home).items.any { it.playlistId == playlistId && it.url == live.url })
                        check(home.findViewById<RecyclerView>(R.id.items).hasFocus())
                        val television = home.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK ==
                            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
                        if (television) check(MainActivity::class.java.getDeclaredField("liveLevel").let {
                            it.isAccessible = true; it.get(home).toString() == "GUIDE" })
                    }
                } catch (failure: Throwable) {
                    capture("cold-guide-failure")
                    var diagnostic = ""
                    main {
                        fun field(name: String): Any? = runCatching { MainActivity::class.java.getDeclaredField(name).let {
                            it.isAccessible = true; it.get(home) } }.getOrNull()
                        val items = home.findViewById<RecyclerView>(R.id.items)
                        val holder = items.findViewHolderForAdapterPosition(grid(home).focusedRow)?.itemView
                        val focused = home.currentFocus
                        diagnostic = JSONObject().put("requestedPlaylist", playlistId).put("browsingPlaylist", browsingId)
                            .put("activePlaylist", app.settings.activePlaylistId).put("mainPlaylist", (field("playlist") as? Playlist)?.id)
                            .put("section", field("section")?.toString()).put("pendingGuide", field("pendingGuide") != null)
                            .put("pendingGuidePlaylist", field("pendingGuidePlaylistId"))
                            .put("selectedGroup", field("selectedGroup") ?: JSONObject.NULL)
                            .put("focusedGroup", field("focusedGroup") ?: JSONObject.NULL)
                            .put("loadedGroup", field("loadedGroup") ?: JSONObject.NULL)
                            .put("liveLevel", field("liveLevel")?.toString())
                            .put("loadJobActive", (field("loadJob") as? kotlinx.coroutines.Job)?.isActive)
                            .put("listLoadGeneration", field("listLoadGeneration"))
                            .put("intentGuidePlaylist", home.intent.getLongExtra(MainActivity.EXTRA_GUIDE_PLAYLIST, 0L))
                            .put("intentHasGuideChannel", home.intent.hasExtra(MainActivity.EXTRA_GUIDE_CHANNEL))
                            .put("rowPlaylists", JSONArray(grid(home).items.map { it.playlistId }))
                            .put("currentFocusClass", focused?.javaClass?.simpleName).put("currentFocusId", focused?.id)
                            .put("itemsInTouchMode", items.isInTouchMode).put("itemsLaidOut", items.isLaidOut)
                            .put("itemsLayoutRequested", items.isLayoutRequested).put("itemsWindowFocus", items.hasWindowFocus())
                            .put("itemsHasFocus", items.hasFocus()).put("holderPresent", holder != null)
                            .put("holderLaidOut", holder?.isLaidOut).put("holderFocusable", holder?.isFocusable)
                            .put("holderFocusableInTouchMode", holder?.isFocusableInTouchMode).put("holderShown", holder?.isShown)
                            .put("inputGeneration", field("userInputGeneration"))
                            .put("gridNavigationGeneration", LiveGuideGrid::class.java.getDeclaredField("navigationGeneration").let {
                                it.isAccessible = true; it.getLong(grid(home)) })
                            .put("focusedRow", grid(home).focusedRow).put("focusTime", time(home, "focusTime"))
                            .put("windowStart", time(home, "windowStart")).put("now", System.currentTimeMillis())
                            .put("lifecycle", (home as androidx.appcompat.app.AppCompatActivity).lifecycle.currentState.name)
                            .put("windowFocus", home.hasWindowFocus()).put("fullscreenOpens", coldMonitor.hits).toString()
                        // Probe only after saving the failing state/capture. It never changes the cold-case verdict.
                        diagnostic = JSONObject(diagnostic).put("lateFocusRequest", holder?.requestFocus())
                            .put("lateItemsHasFocus", items.hasFocus()).toString()
                    }
                    throw IllegalStateException("Cold guide state: $diagnostic", failure)
                }
                main { check(grid(home).items[grid(home).focusedRow].let { it.playlistId == playlistId && it.url == live.url }) }
                assertNow(home)
                check(coldMonitor.hits == 0)
                capture("cold-guide-source-a-now")
            }
            removeMonitor(coldMonitor)
            app.settings.resumeLastChannel = false
            openGuide(home)
            verify("fresh guide opens on the current programme and a window containing now") { assertNow(home); capture("guide-current") }
            verify("future browsing survives ordinary guide refresh and explicit reopen returns to now") {
                repeat(3) { sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT) }
                await { time(home, "focusTime") > System.currentTimeMillis() + 30 * 60_000 }
                val future = time(home, "focusTime")
                main { grid(home).refresh() }
                check(time(home, "focusTime") == future)
                main { grid(home).reloadCommitted() }
                SystemClock.sleep(500)
                check(time(home, "focusTime") == future)
                capture("guide-future-browsing")
                val playerMonitor = addMonitor(PlayerActivity::class.java.name, null, false)
                try {
                    main { home.startActivity(Intent(home, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(MainActivity.EXTRA_GUIDE_CHANNEL, live.url).putExtra(MainActivity.EXTRA_GUIDE_GROUP, live.group)
                        .putExtra(MainActivity.EXTRA_GUIDE_PLAYLIST, playlistId)) }
                    assertNow(home)
                    SystemClock.sleep(1000)
                    assertNow(home)
                    check(playerMonitor.hits == 0)
                    capture("warm-explicit-guide-now")
                } finally { removeMonitor(playerMonitor) }
                assertNow(home); capture("guide-reopened-now")
            }
            verify("past browsing is retained while active and explicit reopen returns to now") {
                repeat(2) { sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_REWIND) }
                await { time(home, "focusTime") < System.currentTimeMillis() - 30 * 60_000 }
                val past = time(home, "focusTime")
                main { grid(home).refresh() }
                check(time(home, "focusTime") == past)
                capture("guide-past-browsing")
                nav(home, "Movies"); openGuide(home)
                assertNow(home)
            }
            main { app.livePlayback.select(home as androidx.appcompat.app.AppCompatActivity, live, listOf(live)) {} }
            await { app.livePlayback.player?.playbackState == Player.STATE_READY }
            main { app.livePlayback.player?.pause() }
            nav(home, "Movies")
            verify("held BACK from browsing restores current fullscreen stream and its release does not exit") {
                await { Nav.hasCurrentStream(home) }
                finishScreen(holdToPlayer(live, "held-back-from-browse"))
                await { home.hasWindowFocus() }
            }
            verify("short BACK from Settings keeps its normal return without opening fullscreen") {
                check(!app.settings.settingsLockEnabled)
                val settings = openSettings(home)
                val monitor = addMonitor(PlayerActivity::class.java.name, null, false)
                try {
                    sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                    await { settings.isFinishing || settings.isDestroyed }
                    await { home.hasWindowFocus() }
                    check(monitor.hits == 0)
                } finally { removeMonitor(monitor) }
            }
            verify("held BACK from Settings returns to the current stream without a second release action") {
                val settings = openSettings(home)
                finishScreen(holdToPlayer(live, "held-back-from-settings"))
                if (!settings.isDestroyed && !settings.isFinishing) finishScreen(settings)
                await { home.hasWindowFocus() }
            }
            verify("held BACK from public locked Settings returns without exposing protected Settings") {
                main { app.settings.setSettingsLockEnabled(true) }
                val protectedMonitor = addMonitor(SettingsActivity::class.java.name, null, false)
                try {
                    nav(home, "Settings")
                    await { uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Playback")?.isNotEmpty() == true }
                    check(protectedMonitor.hits == 0)
                    finishScreen(holdToPlayer(live, "held-back-from-public-settings"))
                } finally { removeMonitor(protectedMonitor); main { app.settings.setSettingsLockEnabled(false) } }
                await { home.hasWindowFocus() }
            }
            verify("changed source prevents held BACK from reopening a stale current stream") {
                main { check(Nav.hasCurrentStream(home)); app.livePlayback.stop(); check(Nav.hasCurrentStream(home)) }
                val source = app.repo.playlist(playlistId)!!
                try {
                    app.repo.savePlaylist(source.copy(url = source.url + "?different_source=1"))
                    main { check(!Nav.hasCurrentStream(home)) }
                    val settings = openSettings(home)
                    val monitor = addMonitor(PlayerActivity::class.java.name, null, false)
                    try {
                        heldBack()
                        SystemClock.sleep(500)
                        check(monitor.hits == 0)
                        main { check(app.livePlayback.player?.isPlaying != true) }
                        if (!settings.isFinishing && !settings.isDestroyed) finishScreen(settings)
                    } finally { removeMonitor(monitor) }
                } finally { app.repo.savePlaylist(source) }
            }
        } catch (error: Throwable) {
            failed++; results.put(JSONObject().put("name", "Navigation fixture execution").put("passed", false)
                .put("type", error.javaClass.simpleName).put("message", error.message.orEmpty().take(400))
                .put("stack", error.stackTrace.take(8).joinToString("\n")))
        } finally {
            main { opened.asReversed().filter { !it.isDestroyed }.forEach { it.finish() }; app.livePlayback.stop(); YourIptvApp.playQueue = oldQueue }
            waitForIdleSync()
            if (playlistId != 0L) app.repo.deletePlaylist(playlistId)
            if (browsingId != 0L) app.repo.deletePlaylist(browsingId)
            val editor = preferences.edit()
            original.forEach { (key, value) -> when (value) {
                null -> editor.remove(key)
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
            } }
            check(editor.commit())
        }
        finish(if (failed == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
            putString("results", results.toString()); putInt("failures", failed)
        })
    }
}
