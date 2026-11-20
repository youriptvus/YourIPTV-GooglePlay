package com.youriptv.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.youriptv.app.data.*
import com.youriptv.app.ui.*
import org.json.JSONArray
import org.json.JSONObject

/** Actual Media3/detail-screen checks on a private loopback fixture, never on customer data. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VodUiInstrumentation : Instrumentation() {
    private val results = JSONArray()
    private val opened = ArrayList<Activity>()
    private var failed = 0
    private fun capture(name: String) {
        val bitmap = uiAutomation.takeScreenshot() ?: return
        val directory = java.io.File(targetContext.getExternalFilesDir(null), "vod-qa").apply { mkdirs() }
        java.io.File(directory, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun onMain(action: () -> Unit) {
        var failure: Throwable? = null
        runOnMainSync { try { action() } catch (error: Throwable) { failure = error } }
        failure?.let { throw it }
    }
    private fun await(timeout: Long = 15_000, condition: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < end) {
            var ready = false; onMain { ready = condition() }
            if (ready) return
            SystemClock.sleep(100)
        }
        error("UI condition timed out")
    }
    private fun checkStep(name: String, action: () -> Unit) {
        try { action(); results.put(JSONObject().put("name", name).put("passed", true)) }
        catch (e: Throwable) { failed++; results.put(JSONObject().put("name", name).put("passed", false).put("type", e.javaClass.simpleName)) }
    }
    private fun launch(type: Class<out Activity>, item: MediaItem): Activity {
        return startActivitySync(Intent(targetContext, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("playlist", item.playlistId).putExtra("url", item.url).putExtra("name", item.name)
            .putExtra("group", item.group).putExtra("logo", item.logo).putExtra("remote_id", item.remoteId)).also { screen ->
                opened.add(screen)
                onMain { screen.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
    }
    private fun playClick(details: Activity, id: Int): Activity {
        val monitor = addMonitor(PlayerActivity::class.java.name, null, false)
        val television = targetContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        if (television) onMain { details.findViewById<Button>(id).performClick() }
        else {
            val center = IntArray(2)
            onMain {
                val button = details.findViewById<Button>(id)
                button.requestRectangleOnScreen(android.graphics.Rect(0, 0, button.width, button.height), true)
                button.getLocationOnScreen(center)
                center[0] += button.width / 2; center[1] += button.height / 2
            }
            val time = SystemClock.uptimeMillis()
            for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                val event = android.view.MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, center[0].toFloat(), center[1].toFloat(), 0)
                event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                sendPointerSync(event); event.recycle()
            }
        }
        return try { (waitForMonitorWithTimeout(monitor, 10_000) ?: error("Player did not open")).also { opened.add(it) } }
        finally { removeMonitor(monitor) }
    }
    private fun player(screen: Activity): ExoPlayer {
        var value = screen.findViewById<PlayerView>(R.id.player_view).player
        while (value is ForwardingPlayer) value = value.wrappedPlayer
        return value as ExoPlayer
    }
    private fun ready(screen: Activity) = await { player(screen).let {
        val ready = it.playbackState == Player.STATE_READY && it.duration > 0 && it.playerError == null
        if (ready) it.pause() // Inspect the resume position before test-runner scheduling can advance a short fixture.
        ready
    } }
    private fun finishScreen(screen: Activity) { onMain { screen.finish() }; waitForIdleSync(); SystemClock.sleep(300) }
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        if (targetContext.packageName != "com.youriptv.app.playtest") {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("error", "Isolated package required") }); return
        }
        waitForIdleSync()
        val app = YourIptvApp.instance
        var pid = 0L
        val oldAuto = app.settings.autoPlayNextEpisode
        try {
            // Recover only this harness's exact synthetic fixtures after an interrupted test run.
            app.repo.playlists().filter { it.name.startsWith("VOD UI QA ") && it.type == SourceType.XTREAM &&
                it.url == "http://127.0.0.1:18766" && it.username == "fixture" && it.password == "fixture" }
                .forEach { app.repo.deletePlaylist(it.id) }
            pid = app.repo.savePlaylist(Playlist(name = "VOD UI QA ${System.nanoTime()}", type = SourceType.XTREAM,
                url = "http://127.0.0.1:18766", username = "fixture", password = "fixture", refreshHours = 0))
            app.repo.refresh(app.repo.playlist(pid)!!, {})
            val movie = app.repo.items(pid, ItemKind.MOVIE, null).single()
            val series = app.repo.items(pid, ItemKind.SERIES, null).single()
            var movieDetails = launch(MovieDetailsActivity::class.java, movie)
            checkStep("movie details tolerate missing artwork and show provider metadata") {
                await { movieDetails.findViewById<TextView>(R.id.movie_plot).text.contains("QA movie metadata") }
                onMain { check(movieDetails.findViewById<TextView>(R.id.movie_metadata).text.contains("2026")) }
                capture("movie-details")
            }
            var screen = playClick(movieDetails, R.id.movie_play)
            ready(screen)
            checkStep("movie playback renders decoded frames and pause/seek saves position") {
                await { player(screen).videoDecoderCounters?.let { it.ensureUpdated(); it.renderedOutputBufferCount > 0 } == true }
                onMain { player(screen).pause(); player(screen).seekTo(2500) }
                await { app.repo.resumePosition(movie) in 2400..2700 }
            }
            if (targetContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK !=
                android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) checkStep("phone rotation preserves the same player and saved position") {
                var originalPlayer: ExoPlayer? = null
                onMain { originalPlayer = player(screen) }
                onMain { screen.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                await { screen.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
                onMain { check(player(screen) === originalPlayer); check(player(screen).currentPosition in 2400..2700) }
                // Configuration changes precede the window/surface rotation animation.
                // Capture the settled layout, rather than an intermediate compositor frame.
                SystemClock.sleep(700)
                capture("phone-player-landscape")
                onMain { screen.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
                await { screen.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT }
            }
            checkStep("periodic progress saves while playback remains open") {
                onMain { player(screen).seekTo(1000); player(screen).setPlaybackSpeed(0.1f) }
                await { app.repo.resumePosition(movie) in 900..1100 }
                onMain { player(screen).play() }
                await(8_000) { player(screen).isPlaying && app.repo.resumePosition(movie) >= 1300 }
                onMain { player(screen).pause(); player(screen).setPlaybackSpeed(1f); player(screen).seekTo(2500) }
                await { app.repo.resumePosition(movie) in 2400..2700 }
            }
            finishScreen(screen)
            checkStep("returning to movie shows Resume and Play from beginning") {
                await { movieDetails.findViewById<Button>(R.id.movie_play).text.startsWith("Resume movie") }
                onMain { check(movieDetails.findViewById<Button>(R.id.movie_beginning).visibility == android.view.View.VISIBLE) }
                capture("movie-resume")
            }
            screen = playClick(movieDetails, R.id.movie_play); ready(screen)
            checkStep("movie resumes exact saved position") {
                onMain { player(screen).pause(); check(player(screen).currentPosition in 2400..3800) }
            }
            finishScreen(screen)
            screen = playClick(movieDetails, R.id.movie_beginning); ready(screen)
            checkStep("explicit movie restart starts near zero") { onMain { player(screen).pause(); check(player(screen).currentPosition < 1500) } }
            finishScreen(screen)
            checkStep("movie watched and clear actions update Continue Watching") {
                onMain { movieDetails.findViewById<Button>(R.id.movie_watched).performClick() }
                check(app.repo.watchProgress(movie)?.completed == true)
                check(app.repo.continueWatching(pid, ItemKind.MOVIE).isEmpty())
                onMain { movieDetails.findViewById<Button>(R.id.movie_clear).performClick() }
                check(app.repo.resumePosition(movie) == 0L && app.repo.watchProgress(movie)?.completed == false)
            }
            finishScreen(movieDetails)
            val details = app.repo.seriesDetails(app.repo.playlist(pid)!!, series)
            val queue = SeriesBrowseState.playbackQueue(series, details.episodes)
            val episode = queue[1]
            app.repo.saveWatchProgress(episode, 2500, 6024)
            val seriesDetails = launch(SeriesActivity::class.java, series)
            checkStep("series identifies saved episode across season rails") {
                await { seriesDetails.findViewById<Button>(R.id.series_continue).text.toString() == "Resume S1 E2" }
                await { seriesDetails.findViewById<TextView>(R.id.series_selected).text.contains("Season 1 · Episode 2") }
                onMain { check(seriesDetails.findViewById<TextView>(R.id.series_metadata).text.contains("2 seasons")) }
                capture("series-resume")
            }
            checkStep("D-pad switches seasons and restores each rail selection") {
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
                await { seriesDetails.findViewById<TextView>(R.id.series_selected).text.contains("Season 2 · Episode 1") }
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_UP)
                await { seriesDetails.findViewById<TextView>(R.id.series_selected).text.contains("Season 1 · Episode 2") }
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_LEFT)
                await { seriesDetails.findViewById<TextView>(R.id.series_selected).text.contains("Season 1 · Episode 1") }
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_RIGHT)
                await { seriesDetails.findViewById<TextView>(R.id.series_selected).text.contains("Season 1 · Episode 2") }
            }
            screen = playClick(seriesDetails, R.id.series_continue); ready(screen)
            checkStep("episode resumes exact episode with next-season queue available") {
                onMain { player(screen).pause(); check(player(screen).currentMediaItem?.mediaId == episode.url)
                    check(player(screen).currentPosition >= 2400)
                    check(screen.findViewById<PlayerView>(R.id.player_view).player!!.hasNextMediaItem()) }
            }
            app.settings.autoPlayNextEpisode = true
            onMain { player(screen).seekTo(player(screen).duration - 250); player(screen).play() }
            fun dialog(): androidx.appcompat.app.AlertDialog? = PlayerActivity::class.java.getDeclaredField("nextDialog").let {
                it.isAccessible = true; it.get(screen) as? androidx.appcompat.app.AlertDialog
            }
            checkStep("natural episode end offers a cancellable next-episode countdown") {
                await { dialog()?.isShowing == true }
                capture("next-countdown")
                onMain { dialog()!!.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick() }
                SystemClock.sleep(10_500)
                onMain { check(player(screen).currentMediaItem?.mediaId == episode.url) }
                check(app.repo.watchProgress(episode)?.completed == true)
            }
            checkStep("returning from background preserves completed episode choices without replay") {
                val taskId = screen.taskId
                onMain { check(screen.moveTaskToBack(true)) }
                SystemClock.sleep(700)
                val manager = targetContext.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val task = manager.appTasks.first { it.taskInfo.taskId == taskId }
                onMain { task.moveToFront() }
                await { dialog()?.isShowing == true }
                onMain { check(!player(screen).isPlaying); check(player(screen).currentMediaItem == null)
                    dialog()!!.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick() }
                check(app.repo.watchProgress(episode)?.completed == true)
            }
            // Restart through the same explicit user action and let the normal END event drive the transition.
            onMain { Nav.play(screen, queue, 1, fromBeginning = true) }; ready(screen)
            onMain { player(screen).seekTo(player(screen).duration - 250); player(screen).play() }
            checkStep("auto next crosses from season one to season two after countdown") {
                await(20_000) {
                    val ready = player(screen).currentMediaItem?.mediaId == queue[2].url && player(screen).playbackState == Player.STATE_READY
                    if (ready) player(screen).pause()
                    ready
                }
                onMain { player(screen).pause(); player(screen).seekTo(2000) }
            }
            finishScreen(screen)
            checkStep("return from playback restores the new season and exact episode") {
                await { seriesDetails.findViewById<TextView>(R.id.series_selected).text.contains("Season 2 · Episode 1") }
                capture("series-return-focus")
            }
            checkStep("Continue Watching opens the exact next-season episode with resume choices") {
                val savedEntry = app.repo.continueWatching(pid, ItemKind.SERIES).single()
                check(savedEntry.url == queue[2].url && savedEntry.season == 2 && savedEntry.episode == 1)
                val detailsMonitor = addMonitor(SeriesActivity::class.java.name, null, false)
                onMain { Nav.open(seriesDetails, savedEntry, listOf(savedEntry)) }
                val reopened = try { waitForMonitorWithTimeout(detailsMonitor, 10_000) ?: error("Series did not reopen") }
                    finally { removeMonitor(detailsMonitor) }
                opened.add(reopened)
                await { reopened.findViewById<TextView>(R.id.series_selected).text.contains("Season 2 · Episode 1") }
                val playMonitor = addMonitor(PlayerActivity::class.java.name, null, false)
                try {
                    val until = SystemClock.elapsedRealtime() + 10_000
                    var resumeNode: android.view.accessibility.AccessibilityNodeInfo? = null
                    while (resumeNode == null && SystemClock.elapsedRealtime() < until) {
                        resumeNode = uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Resume episode")?.firstOrNull()
                        if (resumeNode == null) SystemClock.sleep(100)
                    }
                    check(resumeNode?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true)
                    val resumed = waitForMonitorWithTimeout(playMonitor, 10_000) ?: error("Resume choice did not open player")
                    opened.add(resumed); ready(resumed)
                    onMain { player(resumed).pause(); check(player(resumed).currentMediaItem?.mediaId == queue[2].url)
                        check(player(resumed).currentPosition >= 1900) }
                    finishScreen(resumed)
                } finally { removeMonitor(playMonitor); finishScreen(reopened) }
            }
            finishScreen(seriesDetails)
            app.repo.refresh(app.repo.playlist(pid)!!, {})
            checkStep("catalogue refresh preserves watch progress") { check(app.repo.watchProgress(episode)?.completed == true) }
            val originalSource = app.repo.playlist(pid)!!
            checkStep("an edited account revokes stale movie details") {
                val stale = launch(MovieDetailsActivity::class.java, movie)
                await { stale.hasWindowFocus() }
                app.repo.savePlaylist(originalSource.copy(username = "another-fixture-account"))
                onMain { stale.findViewById<Button>(R.id.movie_play).performClick() }
                await { stale.isFinishing || stale.isDestroyed }
            }
            app.repo.savePlaylist(originalSource)
            checkStep("an edited account revokes stale series details") {
                val stale = launch(SeriesActivity::class.java, series)
                await { stale.findViewById<Button>(R.id.series_continue).text.contains(Regex("S[0-9]+ E[0-9]+")) && stale.hasWindowFocus() }
                app.repo.savePlaylist(originalSource.copy(username = "another-fixture-account"))
                onMain { stale.findViewById<Button>(R.id.series_continue).performClick() }
                await { stale.isFinishing || stale.isDestroyed }
            }
            app.repo.savePlaylist(originalSource)
        } catch (e: Throwable) {
            failed++; results.put(JSONObject().put("name", "UI fixture execution").put("passed", false).put("type", e.javaClass.simpleName))
        } finally {
            onMain { opened.asReversed().filter { !it.isDestroyed }.forEach { it.finish() }; app.settings.autoPlayNextEpisode = oldAuto }
            waitForIdleSync()
            if (pid != 0L) app.repo.deletePlaylist(pid)
        }
        finish(if (failed == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
            putString("results", results.toString()); putInt("failures", failed)
        })
    }
}
