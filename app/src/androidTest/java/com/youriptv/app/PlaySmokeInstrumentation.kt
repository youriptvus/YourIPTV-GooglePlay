package com.youriptv.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.graphics.Rect
import android.graphics.Bitmap
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.InputDevice
import android.view.LayoutInflater
import android.view.accessibility.AccessibilityNodeInfo
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.youriptv.app.data.PinCheck
import com.youriptv.app.data.PinKind
import com.youriptv.app.data.Settings
import com.youriptv.app.data.SettingsLockPolicy
import com.youriptv.app.data.Playlist
import com.youriptv.app.data.SourceType
import com.youriptv.app.data.ItemKind
import com.youriptv.app.data.MediaItem
import com.youriptv.app.ui.AboutActivity
import com.youriptv.app.ui.MainActivity
import com.youriptv.app.ui.PrivacyActivity
import com.youriptv.app.ui.SettingsActivity
import com.youriptv.app.ui.SettingsCategory
import com.youriptv.app.ui.PlayerActivity
import androidx.media3.common.Player
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONArray
import org.json.JSONObject

/** Framework-only device checks; refuses to run against the customer application. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaySmokeInstrumentation : Instrumentation() {
    private val results = JSONArray()
    private val opened = mutableListOf<Activity>()
    private var failed = 0
    private var fixtureBase = ""
    private var fixtureStats: JSONObject? = null
    private var runId = ""
    private var soakSeconds = 0
    private var soakStats: JSONObject? = null

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        fixtureBase = arguments?.getString("fixtureBase").orEmpty().trimEnd('/')
        runId = arguments?.getString("runId").orEmpty()
        soakSeconds = arguments?.getString("soakSeconds")?.toIntOrNull()?.coerceIn(0, 900) ?: 0
        start()
    }

    override fun onStart() {
        if (targetContext.packageName != "com.youriptv.app.playtest") {
            finish(Activity.RESULT_CANCELED, Bundle().apply {
                putString("error", "Device checks require the isolated .playtest package")
            })
            return
        }
        try {
            execute("actual isolated installation defaults Settings Lock OFF") {
                check(!YourIptvApp.instance.settings.isSettingsLocked())
            }
            execute("fresh preferences persist OFF across Settings reconstruction") {
                val context = fixture("fresh")
                check(!Settings(context).settingsLockEnabled)
                check(!Settings(context).settingsLockEnabled)
                check(context.getSharedPreferences("settings", 0).contains(SettingsLockPolicy.LOCAL_KEY))
            }
            execute("explicit lock ON and OFF persist independently of Admin and parental PINs") {
                val context = fixture("choices")
                val first = Settings(context)
                first.pins.setAdmin("738164") // Synthetic test credential, never a built-in production PIN.
                first.pins.set(PinKind.PARENTAL, "3951")
                first.setSettingsLockEnabled(true)
                check(Settings(context).settingsLockEnabled)
                first.setSettingsLockEnabled(false)
                val restarted = Settings(context)
                check(!restarted.settingsLockEnabled)
                check(restarted.pins.checkSettingsMaster("738164") == PinCheck.Ok)
                check(restarted.pins.check(PinKind.PARENTAL, "3951") == PinCheck.Ok)
            }
            execute("existing explicit lock ON survives upgrade initialization") {
                val context = fixture("upgrade_on")
                context.getSharedPreferences("settings", 0).edit().putBoolean(SettingsLockPolicy.LOCAL_KEY, true).commit()
                check(Settings(context).settingsLockEnabled)
            }
            execute("existing explicit lock OFF survives upgrade initialization") {
                val context = fixture("upgrade_off")
                context.getSharedPreferences("settings", 0).edit().putBoolean(SettingsLockPolicy.LOCAL_KEY, false).commit()
                check(!Settings(context).settingsLockEnabled)
            }
            execute("unknown existing lock provenance remains protected") {
                val context = fixture("upgrade_unknown")
                context.getSharedPreferences("settings", 0).edit().putBoolean("resume_last", true).commit()
                check(Settings(context).settingsLockEnabled)
            }
            execute("new installations require local Admin setup with no shared default") {
                val s = Settings(fixture("admin_setup"))
                check(s.pins.needsAdminSetup())
                check(s.pins.checkSettingsMaster("000000") is PinCheck.Wrong)
                s.pins.setAdmin("738164")
                check(!s.pins.needsAdminSetup())
                check(s.pins.checkSettingsMaster("738164") == PinCheck.Ok)
            }
            execute("custom Settings PIN cannot authorize Admin and parental lockout is independent") {
                val s = Settings(fixture("admin_boundaries"))
                s.pins.setAdmin("738164")
                s.pins.set(PinKind.SETTINGS, "6042")
                s.pins.set(PinKind.PARENTAL, "3951")
                check(s.pins.checkSettingsAccess("6042") == PinCheck.Ok)
                check(s.pins.checkSettingsMaster("6042") is PinCheck.Wrong)
                repeat(4) { s.pins.checkSettingsMaster("000000") }
                check(s.pins.lockoutRemaining(PinKind.SETTINGS) > 0)
                check(s.pins.checkSettingsMaster("738164") is PinCheck.LockedOut)
                check(s.pins.check(PinKind.PARENTAL, "3951") == PinCheck.Ok)
                check(s.pins.lockoutRemaining(PinKind.PARENTAL) == 0L)
            }
            execute("installed Play manifest has no self-installer or overlay permission") {
                @Suppress("DEPRECATION")
                val permissions = targetContext.packageManager.getPackageInfo(targetContext.packageName,
                    PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
                for (forbidden in listOf("REQUEST_INSTALL_PACKAGES", "UPDATE_PACKAGES_WITHOUT_USER_ACTION", "SYSTEM_ALERT_WINDOW"))
                    check("android.permission.$forbidden" !in permissions)
            }
            execute("shared browser opens and responds to D-pad focus") {
                val main = launch(MainActivity::class.java)
                waitForIdleSync()
                sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
                waitForIdleSync()
                runOnMainSync {
                    check(main.findViewById<View>(R.id.nav_container).isShown)
                    check(main.currentFocus != null)
                }
            }
            if (!isTelevision()) {
                execute("phone touchscreen opens normal Settings from the shared browser") {
                    val main = launch(MainActivity::class.java)
                    waitForIdleSync()
                    var settings: View? = null
                    runOnMainSync {
                        val nav = main.findViewById<ViewGroup>(R.id.nav_container)
                        settings = (0 until nav.childCount).map { nav.getChildAt(it) }
                            .filterIsInstance<TextView>().single { it.text.toString() == "Settings" }
                        settings!!.requestRectangleOnScreen(Rect(0, 0, settings!!.width, settings!!.height), true)
                    }
                    waitForIdleSync()
                    val monitor = addMonitor(SettingsActivity::class.java.name, null, false)
                    try {
                        touch(settings!!)
                        val screen = monitor.waitForActivityWithTimeout(5_000) ?: error("Touch did not open Settings")
                        opened += screen
                        waitForIdleSync()
                        runOnMainSync { check(screen.findViewById<TextView>(R.id.list_title).text.toString() == "Settings") }
                    } finally { removeMonitor(monitor) }
                }
                execute("phone screen recreates correctly when switching portrait and landscape") {
                    var screen = launch(AboutActivity::class.java)
                    val previous = screen.requestedOrientation
                    val wanted = if (screen.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                        Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
                    val monitor = addMonitor(AboutActivity::class.java.name, null, false)
                    try {
                        runOnMainSync {
                            screen.requestedOrientation = if (wanted == Configuration.ORIENTATION_LANDSCAPE)
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                        screen = monitor.waitForActivityWithTimeout(8_000) ?: error("Rotation did not recreate the screen")
                        opened += screen
                        waitForIdleSync()
                        check(screen.resources.configuration.orientation == wanted)
                        runOnMainSync { check(screen.findViewById<TextView>(R.id.about_title).text.toString().startsWith("About")) }
                    } finally {
                        runOnMainSync { if (!screen.isDestroyed) screen.requestedOrientation = previous }
                        monitor.waitForActivityWithTimeout(5_000)?.let { opened += it }
                        removeMonitor(monitor)
                    }
                }
            }
            execute("normal Settings UI opens directly without a PIN") {
                val screen = launch(SettingsActivity::class.java)
                waitForIdleSync()
                runOnMainSync {
                    check(screen.findViewById<TextView>(R.id.list_title).text.toString() == "Settings")
                    check((screen.findViewById<RecyclerView>(R.id.list).adapter?.itemCount ?: 0) > 0)
                }
                capture("settings")
            }
            execute("Settings Lock enables and disables through authenticated Admin UI") {
                val app = YourIptvApp.instance
                check(!app.settings.isSettingsLocked())
                val enroll = app.settings.pins.needsAdminSetup()
                var screen = launch(SettingsActivity::class.java)
                openLockPage(screen)
                chooseLockToggle("Turn on Settings lock")
                enterDialogPin("738164")
                if (enroll) enterDialogPin("738164")
                await(5_000) { app.settings.isSettingsLocked() && screen.isFinishing }
                screen = launch(SettingsActivity::class.java)
                enterDialogPin("738164")
                await(5_000) { screen.findViewById<TextView>(R.id.list_title).text.toString() == "Unlock Settings" }
                openLockPage(screen)
                chooseLockToggle("Turn off Settings lock")
                enterDialogPin("738164")
                await(5_000) { !app.settings.isSettingsLocked() && screen.isFinishing }
            }
            execute("About opens offline privacy policy inside the same app") {
                val about = launch(AboutActivity::class.java)
                waitForIdleSync()
                capture("about")
                val monitor = addMonitor(PrivacyActivity::class.java.name, null, false)
                try {
                    runOnMainSync {
                        check(about.findViewById<TextView>(R.id.about_version).text.contains("1.0.9"))
                        check(about.findViewById<Button>(R.id.about_privacy).performClick())
                    }
                    val privacy = monitor.waitForActivityWithTimeout(5_000) ?: error("Privacy screen did not open")
                    opened += privacy
                    waitForIdleSync()
                    runOnMainSync {
                        check(privacy.findViewById<TextView>(R.id.about_title).text.toString() == "Privacy policy")
                        check(privacy.findViewById<TextView>(R.id.about_body).text.length > 200)
                    }
                    capture("privacy")
                } finally { removeMonitor(monitor) }
            }
            execute("phone portrait resources inflate with shared browser controls") {
                checkPortraitResources(360, 760)
            }
            execute("tablet portrait resources inflate with shared browser controls") {
                checkPortraitResources(800, 1280)
            }
            if (fixtureBase.isNotEmpty()) playbackFixtureTests()
        } finally {
            runOnMainSync { opened.asReversed().forEach { if (!it.isFinishing && !it.isDestroyed) it.finish() } }
            val report = JSONObject().put("package", targetContext.packageName)
                .put("runId", runId)
                .put("tests", results.length()).put("failures", failed).put("results", results)
                .put("fixtureStats", fixtureStats)
                .put("soakStats", soakStats)
                .put("limitations", "Settings reconstruction tests are not process death. Phone/tablet resource inflation on TV is not real phone testing. D-pad input is injected through ADB/instrumentation, not a physical remote.")
            targetContext.getExternalFilesDir(null)?.resolve("play-smoke-results.json")?.writeText(report.toString(2))
            finish(if (failed == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
                putInt("tests", results.length())
                putInt("failures", failed)
                putString("results", report.toString())
            })
        }
    }

    private fun execute(name: String, action: () -> Unit) {
        val record = JSONObject().put("name", name)
        try { action(); record.put("status", "passed") }
        catch (error: Throwable) {
            failed++
            record.put("status", "failed").put("error", error.toString())
        }
        results.put(record)
        sendStatus(0, Bundle().apply { putString("test", name); putString("status", record.getString("status")) })
    }

    private fun openLockPage(screen: Activity) {
        val index = SettingsCategory.SETTINGS_LOCK.ordinal + if (YourIptvApp.instance.settings.isSettingsLocked()) 0 else 2
        val list = screen.findViewById<RecyclerView>(R.id.list)
        waitForIdleSync()
        runOnMainSync { list.layoutManager!!.scrollToPosition(index) }
        await(5_000) { list.findViewHolderForAdapterPosition(index) != null }
        runOnMainSync { check(list.findViewHolderForAdapterPosition(index)!!.itemView.performClick()) }
        await(5_000) { screen.findViewById<TextView>(R.id.list_title).text.toString() == "Settings Lock" }
        waitForIdleSync()
        runOnMainSync { list.layoutManager!!.scrollToPosition(0) }
        waitForIdleSync()
        await(5_000) { list.findViewHolderForAdapterPosition(0) != null }
        runOnMainSync { check(list.findViewHolderForAdapterPosition(0)!!.itemView.performClick()) }
    }

    private fun activeNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val root = uiAutomation.rootInActiveWindow ?: return null
        check(root.packageName?.toString() == targetContext.packageName) { "Refusing dialog automation outside isolated app" }
        fun search(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(node)) return node
            for (index in 0 until node.childCount) node.getChild(index)?.let { search(it)?.let { match -> return match } }
            return null
        }
        return search(root)
    }

    private fun clickDialogText(text: String) {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        while (SystemClock.elapsedRealtime() < deadline) {
            var node = activeNode { it.text?.toString() == text }
            while (node != null) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    waitForIdleSync()
                    return
                }
                node = node.parent
            }
            SystemClock.sleep(100)
        }
        error("Isolated dialog action was not available: $text")
    }

    private fun chooseLockToggle(text: String) = clickDialogText(text)

    private fun enterDialogPin(pin: String) {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        while (SystemClock.elapsedRealtime() < deadline) {
            val entry = activeNode { it.className?.toString() == "android.widget.EditText" }
            if (entry != null) {
                check(entry.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pin)
                }))
                clickDialogText("OK")
                return
            }
            SystemClock.sleep(100)
        }
        error("Isolated Admin PIN dialog was not available")
    }

    /** Only named synthetic preferences inside .playtest are ever reset. */
    private fun fixture(name: String): Context {
        val prefix = "play_smoke_${name}_"
        return object : ContextWrapper(targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                baseContext.getSharedPreferences(prefix + name, mode)
        }.also { it.getSharedPreferences("settings", 0).edit().clear().commit() }
    }

    private fun launch(type: Class<out Activity>): Activity =
        startActivitySync(Intent(targetContext, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            .also { opened += it }

    private fun isTelevision() = (targetContext.resources.configuration.uiMode and
        Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    private fun playbackFixtureTests() {
        require(fixtureBase == "http://127.0.0.1:18765") { "Only the local synthetic test server is allowed" }
        val app = YourIptvApp.instance
        var playlist: Playlist? = null
        var channels: List<MediaItem> = emptyList()
        execute("synthetic M3U Plus playlist imports and refreshes independently") {
            val previous = app.repo.playlists().firstOrNull { it.name == "Play smoke synthetic demo" }
            val id = app.repo.savePlaylist(Playlist(id = previous?.id ?: 0,
                name = "Play smoke synthetic demo", type = SourceType.M3U,
                url = "$fixtureBase/playlist.m3u", epgUrl = "$fixtureBase/epg.xml", refreshHours = 0))
            playlist = app.repo.playlist(id) ?: error("Playlist save failed")
            app.repo.refresh(playlist!!, {})
            playlist = app.repo.playlist(id)!!
            channels = app.repo.items(id, ItemKind.LIVE, null)
            check(channels.size >= 2)
            app.settings.activePlaylistId = id
            check(!app.settings.isSettingsLocked())
        }
        execute("synthetic XMLTV refresh provides integrated programme data") {
            val p = playlist ?: error("Import prerequisite failed")
            check(app.repo.refreshEpg(p, 168, 7, {}) > 0)
            check(channels.any { app.repo.nowAndNext(p.id, it.tvgId).isNotEmpty() })
        }
        execute("shared live guide displays imported channels and accepts remote navigation") {
            check(channels.size >= 2)
            val main = launch(MainActivity::class.java)
            await(10_000) {
                (main.findViewById<RecyclerView>(R.id.items).adapter?.itemCount ?: 0) >= 2
            }
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
            runOnMainSync { check(main.currentFocus != null) }
            capture("synthetic-guide")
        }
        if (channels.size < 2) return
        var screen: Activity? = null
        val rendered = AtomicInteger()
        val ended = AtomicInteger()
        val errors = AtomicInteger()
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { rendered.incrementAndGet() }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) ended.incrementAndGet() }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { errors.incrementAndGet() }
        }
        execute("one shared Media3 player renders synthetic live MPEG-TS") {
            runOnMainSync { YourIptvApp.playQueue = channels }
            screen = launch(PlayerActivity::class.java)
            runOnMainSync { app.livePlayback.player?.addListener(listener) ?: error("Shared player was not attached") }
            await(20_000) { app.livePlayback.player?.playbackState == Player.STATE_READY && rendered.get() > 0 }
            runOnMainSync { check(app.livePlayback.current?.url == channels[0].url) }
            capture("synthetic-player")
        }
        if (screen == null) return
        execute("RIGHT without channel history is safe") {
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            runOnMainSync { check(app.livePlayback.current?.url == channels[0].url) }
        }
        execute("channel switching and RIGHT recall toggle within the current queue") {
            val samePlayer = app.livePlayback.player
            sendKeyDownUpSync(KeyEvent.KEYCODE_CHANNEL_UP)
            await(10_000) { app.livePlayback.current?.url == channels[1].url && app.livePlayback.player?.playbackState == Player.STATE_READY }
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            await(10_000) { app.livePlayback.current?.url == channels[0].url && app.livePlayback.player?.playbackState == Player.STATE_READY }
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            await(10_000) { app.livePlayback.current?.url == channels[1].url && app.livePlayback.player?.playbackState == Player.STATE_READY }
            runOnMainSync { check(app.livePlayback.player === samePlayer) }
        }
        execute("channel overlay consumes RIGHT before previous-channel recall") {
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT)
            await(5_000) { screen!!.findViewById<View>(R.id.channel_overlay).visibility == View.VISIBLE }
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            await(5_000) { screen!!.findViewById<View>(R.id.channel_overlay).visibility == View.GONE }
            runOnMainSync { check(app.livePlayback.current?.url == channels[1].url) }
        }
        if (!isTelevision()) {
            execute("phone fullscreen rotation retains the playing Media3 instance") {
                val player = app.livePlayback.player
                val previous = screen!!.requestedOrientation
                try {
                    runOnMainSync { screen!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                    await(8_000) { screen!!.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
                    runOnMainSync { check(app.livePlayback.player === player && !screen!!.isDestroyed) }
                    await(10_000) { app.livePlayback.player?.playbackState == Player.STATE_READY }
                    runOnMainSync { check(screen!!.findViewById<View>(R.id.player_touch_options).isShown) }
                } finally { runOnMainSync { screen!!.requestedOrientation = previous } }
            }
        }
        execute("fullscreen MPEG-TS recovers repeatedly from synthetic clean stream ends") {
            val baseline = stats()
            val path = URL(channels[1].url).path
            val initial = baseline.getJSONObject("full_gets").optInt(path)
            val endsBefore = ended.get()
            val framesBefore = rendered.get()
            val started = SystemClock.elapsedRealtime()
            var last = baseline
            while (SystemClock.elapsedRealtime() - started < 55_000) {
                last = stats()
                if (last.getJSONObject("full_gets").optInt(path) - initial >= 3 &&
                    ended.get() - endsBefore >= 3 && rendered.get() - framesBefore >= 3) break
                SystemClock.sleep(500)
            }
            fixtureStats = JSONObject().put("before", baseline).put("after", last)
                .put("observedEndedDelta", ended.get() - endsBefore)
                .put("renderedFirstFrameDelta", rendered.get() - framesBefore)
                .put("observationMs", SystemClock.elapsedRealtime() - started)
            check(last.getJSONObject("full_gets").optInt(path) - initial >= 3)
            check(ended.get() - endsBefore >= 3)
            check(rendered.get() - framesBefore >= 3)
            await(10_000) { app.livePlayback.player?.playbackState == Player.STATE_READY }
        }
        if (soakSeconds > 0) execute("bounded ${soakSeconds}s synthetic clean-end playback observation") {
            val baseline = stats()
            val path = URL(channels[1].url).path
            val opensBefore = baseline.getJSONObject("full_gets").optInt(path)
            val endedBefore = ended.get()
            val framesBefore = rendered.get()
            val errorsBefore = errors.get()
            val started = SystemClock.elapsedRealtime()
            var lastPlaying = started
            var longestNotPlayingMs = 0L
            var playingSamples = 0
            var totalSamples = 0
            val observations = JSONArray()
            while (SystemClock.elapsedRealtime() - started < soakSeconds * 1_000L) {
                var playing = false
                runOnMainSync {
                    check(!screen!!.isDestroyed && !screen!!.isFinishing)
                    val player = app.livePlayback.player ?: error("Shared player disappeared")
                    check(app.livePlayback.current?.url == channels[1].url)
                    playing = player.isPlaying
                    if (totalSamples % 20 == 0) observations.put(JSONObject()
                        .put("elapsedMs", SystemClock.elapsedRealtime() - started)
                        .put("playbackState", player.playbackState)
                        .put("isPlaying", playing)
                        .put("videoWidth", player.videoFormat?.width ?: 0)
                        .put("videoHeight", player.videoFormat?.height ?: 0))
                }
                val now = SystemClock.elapsedRealtime()
                if (playing) { playingSamples++; lastPlaying = now }
                longestNotPlayingMs = maxOf(longestNotPlayingMs, now - lastPlaying)
                totalSamples++
                SystemClock.sleep(500)
            }
            val after = stats()
            val requiredCycles = maxOf(1, soakSeconds / 18)
            soakStats = JSONObject().put("requestedSeconds", soakSeconds)
                .put("actualDurationMs", SystemClock.elapsedRealtime() - started)
                .put("cleanEndedDelta", ended.get() - endedBefore)
                .put("renderedFirstFrameDelta", rendered.get() - framesBefore)
                .put("fullGetDelta", after.getJSONObject("full_gets").optInt(path) - opensBefore)
                .put("playerErrorDelta", errors.get() - errorsBefore)
                .put("requiredCycles", requiredCycles)
                .put("playingSamples", playingSamples).put("totalSamples", totalSamples)
                .put("longestObservedNotPlayingMs", longestNotPlayingMs)
                .put("samples", observations).put("serverBefore", baseline).put("serverAfter", after)
            check(ended.get() - endedBefore >= requiredCycles)
            check(rendered.get() - framesBefore >= requiredCycles)
            check(after.getJSONObject("full_gets").optInt(path) - opensBefore >= requiredCycles)
            check(errors.get() == errorsBefore)
            check(playingSamples > 0 && longestNotPlayingMs < 30_000)
        }
        runOnMainSync { app.livePlayback.player?.removeListener(listener) }
    }

    /** Poll UI state on the main looper; network and waiting stay off it. */
    private fun await(timeout: Long, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < deadline) {
            var matched = false
            runOnMainSync { matched = condition() }
            if (matched) return
            SystemClock.sleep(100)
        }
        error("Timed out after $timeout ms waiting for UI/playback state")
    }

    private fun stats(): JSONObject {
        val connection = URL("$fixtureBase/stats").openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        return try { connection.inputStream.bufferedReader().use { JSONObject(it.readText()) } }
        finally { connection.disconnect() }
    }

    /** Genuine unedited display captures; no provider credentials or Admin entry is shown. */
    private fun capture(name: String) {
        waitForIdleSync()
        SystemClock.sleep(250)
        val bitmap = uiAutomation.takeScreenshot() ?: error("Raw screenshot capture failed")
        val device = if (isTelevision()) "tv-onn" else "phone-samsung"
        val directory = targetContext.getExternalFilesDir(null)!!.resolve("screenshots").apply { mkdirs() }
        try {
            directory.resolve("$device-playtest-$name.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally { bitmap.recycle() }
    }

    private fun touch(view: View) {
        val position = IntArray(2)
        var x = 0f
        var y = 0f
        runOnMainSync {
            view.getLocationOnScreen(position)
            x = position[0] + view.width / 2f
            y = position[1] + view.height / 2f
        }
        val time = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try { sendPointerSync(event) } finally { event.recycle() }
            SystemClock.sleep(50)
        }
        waitForIdleSync()
    }

    private fun checkPortraitResources(widthDp: Int, heightDp: Int) {
        val configuration = Configuration(targetContext.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            uiMode = Configuration.UI_MODE_TYPE_NORMAL
            screenWidthDp = widthDp
            screenHeightDp = heightDp
            smallestScreenWidthDp = widthDp
        }
        runOnMainSync {
            val themed = ContextThemeWrapper(targetContext.createConfigurationContext(configuration), R.style.Theme_YourIPTV)
            check(themed.resources.getBoolean(R.bool.is_portrait))
            val view = LayoutInflater.from(themed).inflate(R.layout.activity_main, null) as LinearLayout
            check(view.orientation == LinearLayout.VERTICAL)
            check(view.findViewById<View>(R.id.nav_container) != null)
            check(view.findViewById<View>(R.id.items) != null)
        }
    }
}
