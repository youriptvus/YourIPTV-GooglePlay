package com.youriptv.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid
import com.youriptv.app.data.ItemKind
import com.youriptv.app.data.Playlist
import com.youriptv.app.data.SourceType
import com.youriptv.app.ui.AboutActivity
import com.youriptv.app.ui.MainActivity
import com.youriptv.app.ui.PlayerActivity
import com.youriptv.app.ui.PrivacyActivity
import com.youriptv.app.ui.SettingsActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Public reviewer-source and Google ATF audits. Refuses customer packages or private QA data. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayAuditInstrumentation : Instrumentation() {
    private val results = JSONArray()
    private val accessibility = JSONArray()
    private val opened = mutableListOf<Activity>()
    private var failures = 0
    private var runId = ""
    private val base = "https://youriptv.us/review-media/"

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        runId = arguments?.getString("runId").orEmpty()
        start()
    }

    private fun onMain(action: () -> Unit) {
        val error = AtomicReference<Throwable?>()
        runOnMainSync { try { action() } catch (e: Throwable) { error.set(e) } }
        error.get()?.let { throw it }
    }

    private fun test(name: String, action: () -> Unit) {
        val row = JSONObject().put("name", name)
        try { action(); row.put("status", "passed") }
        catch (e: Throwable) {
            failures++
            row.put("status", "failed").put("errorClass", e.javaClass.name)
                .put("reason", e.message.orEmpty().take(400))
        }
        results.put(row)
        sendStatus(0, Bundle().apply { putString("test", name); putString("status", row.getString("status")) })
    }

    private fun launch(type: Class<out Activity>): Activity =
        startActivitySync(Intent(targetContext, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            .also { opened += it; waitForIdleSync() }

    private fun await(timeout: Long, condition: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < until) {
            var ready = false
            onMain { ready = condition() }
            if (ready) return
            SystemClock.sleep(100)
        }
        error("Timed out awaiting the named public-source UI/playback condition")
    }

    private fun audit(screen: Activity, label: String) {
        val findings = JSONArray()
        var errors = 0
        onMain {
            val hierarchy = AccessibilityHierarchyAndroid.newBuilder(screen.window.decorView).build()
            for (check in AccessibilityCheckPreset.getAccessibilityHierarchyChecksForPreset(AccessibilityCheckPreset.LATEST)) {
                for (result in check.runCheckOnHierarchy(hierarchy)) {
                    if (result.type == AccessibilityCheckResultType.NOT_RUN) continue
                    if (result.type == AccessibilityCheckResultType.ERROR) errors++
                    findings.put(JSONObject().put("check", result.sourceCheckClass.simpleName)
                        .put("type", result.type.name).put("resource", result.element?.resourceName)
                        .put("class", result.element?.className?.toString())
                        .put("text", result.element?.text?.toString())
                        .put("bounds", result.element?.boundsInScreen?.toString())
                        .put("message", result.getMessage(Locale.ENGLISH).toString()))
                }
            }
        }
        accessibility.put(JSONObject().put("screen", label).put("errors", errors).put("findings", findings))
        check(errors == 0) { "Google Accessibility Test Framework found $errors errors on $label" }
    }

    override fun onStart() {
        if (targetContext.packageName != "com.youriptv.app.playtest") {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("error", "Isolated QA package required") })
            return
        }
        waitForIdleSync()
        val app = YourIptvApp.instance
        val oldActive = app.settings.activePlaylistId
        val oldResume = app.settings.resumeLastChannel
        var source: Playlist? = null
        try {
            test("existing QA data is exclusively recognized generated test media") {
                check(app.repo.playlists().all { p ->
                    p.username.isBlank() && p.password.isBlank() &&
                        ((p.name == "Play smoke synthetic demo" && p.url == "http://127.0.0.1:18765/playlist.m3u") ||
                         (p.name == "Google Play reviewer demo" && p.url == base + "playlist.m3u"))
                }) { "Unrecognized QA data preserved; refusing automation" }
            }
            if (failures != 0) return
            app.settings.resumeLastChannel = false
            test("public HTTPS reviewer playlist imports two live channels, movie and episode") {
                val previous = app.repo.playlists().firstOrNull { it.name == "Google Play reviewer demo" }
                val id = app.repo.savePlaylist(Playlist(id = previous?.id ?: 0, name = "Google Play reviewer demo",
                    type = SourceType.M3U, url = base + "playlist.m3u", epgUrl = base + "epg.php", refreshHours = 0))
                source = app.repo.playlist(id) ?: error("Reviewer source save failed")
                app.repo.refresh(source!!, {})
                check(app.repo.items(id, ItemKind.LIVE, null).size == 2)
                check(app.repo.items(id, ItemKind.MOVIE, null).isNotEmpty())
                check(app.repo.items(id, ItemKind.EPISODE, null).isNotEmpty())
                app.settings.activePlaylistId = id
            }
            test("public rolling HTTPS XMLTV guide imports current programme data") {
                val p = source ?: error("Reviewer import prerequisite failed")
                check(app.repo.refreshEpg(p, 168, 7, {}) > 0)
                check(app.repo.items(p.id, ItemKind.LIVE, null).all { app.repo.nowAndNext(p.id, it.tvgId).isNotEmpty() })
            }
            for ((type, label) in listOf(MainActivity::class.java to "main", SettingsActivity::class.java to "settings",
                AboutActivity::class.java to "about", PrivacyActivity::class.java to "privacy")) {
                val screen = launch(type)
                test("Google ATF audit of $label screen") { audit(screen, label) }
                if (label == "main") test("keyboard Tab and Shift-Tab expose visible app focus") {
                    sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
                    await(5000) { screen.currentFocus?.isShown == true }
                    sendKeySync(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB, 0, KeyEvent.META_SHIFT_ON))
                    sendKeySync(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB, 0, KeyEvent.META_SHIFT_ON))
                    await(5000) { screen.currentFocus?.isShown == true }
                }
                onMain { screen.finish() }
                waitForIdleSync()
            }
            val p = source ?: return
            for (kind in listOf(ItemKind.LIVE, ItemKind.MOVIE, ItemKind.EPISODE)) {
                test("public HTTPS ${kind.name} renders decoded frames without TLS/player error") {
                    val item = app.repo.items(p.id, kind, null).first()
                    check(item.url.startsWith(base))
                    onMain { YourIptvApp.playQueue = listOf(item) }
                    val screen = launch(PlayerActivity::class.java)
                    val ended = AtomicInteger()
                    var engine: ExoPlayer? = null
                    val listener = object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) ended.incrementAndGet() }
                    }
                    try {
                        await(30_000) {
                            var observed = screen.findViewById<PlayerView>(R.id.player_view).player
                            while (observed is ForwardingPlayer) observed = observed.wrappedPlayer
                            engine = observed as? ExoPlayer
                            engine?.videoDecoderCounters?.ensureUpdated()
                            engine?.let { it.playerError == null && (it.videoDecoderCounters?.renderedOutputBufferCount ?: 0) >= 5 } == true
                        }
                        onMain { engine!!.addListener(listener) }
                        test("Google ATF audit of ${kind.name} player") { audit(screen, kind.name) }
                        if (kind != ItemKind.LIVE) await(15_000) { ended.get() > 0 && (screen.isFinishing || screen.isDestroyed) }
                    } finally {
                        onMain { engine?.removeListener(listener); screen.finish(); app.livePlayback.stop() }
                        waitForIdleSync()
                    }
                }
            }
        } finally {
            onMain { opened.forEach { if (!it.isDestroyed) it.finish() }; app.livePlayback.stop() }
            app.settings.activePlaylistId = oldActive
            app.settings.resumeLastChannel = oldResume
            val report = JSONObject().put("runId", runId).put("package", targetContext.packageName)
                .put("tests", results.length()).put("failures", failures).put("results", results)
                .put("googleAccessibilityFrameworkVersion", "4.1.1").put("accessibility", accessibility)
                .put("smallestScreenWidthDp", targetContext.resources.configuration.smallestScreenWidthDp)
                .put("limitations", "Automated ATF and injected keyboard tests; not manual TalkBack/switch-access/user testing or Google Play delivery. QA package only; generated public media.")
            targetContext.filesDir.resolve("play-public-audit.json").writeText(report.toString(2))
            finish(if (failures == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                Bundle().apply { putInt("tests", results.length()); putInt("failures", failures) })
        }
    }
}
