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
        waitForIdleSync()
        SystemClock.sleep(400) // Let the compositor present the state just verified on the main thread.
        waitForIdleSync()
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
        catch (e: Throwable) { failed++; results.put(JSONObject().put("name", name).put("passed", false)
            .put("type", e.javaClass.simpleName).put("message", e.message.orEmpty().take(1600))
            .put("stack", e.stackTrace.take(8).joinToString("\n"))) }
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
        var browsePid = 0L
        val preferences = targetContext.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val preservedKeys = listOf("auto_next_episode", "active_playlist", "last_channel", "last_live_playlist", "resume_last")
        val oldPreferences = preservedKeys.associateWith { preferences.all[it] }
        val oldQueue = YourIptvApp.playQueue
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
                fun actionState(): String {
                    val button = movieDetails.findViewById<Button>(R.id.movie_watched)
                    val saved = app.repo.watchProgress(movie)
                    val foreground = MovieDetailsActivity::class.java.getDeclaredField("foreground").let {
                        it.isAccessible = true; it.getBoolean(movieDetails) }
                    return JSONObject().put("lifecycle", (movieDetails as androidx.appcompat.app.AppCompatActivity).lifecycle.currentState.name)
                        .put("windowFocus", movieDetails.hasWindowFocus()).put("foreground", foreground)
                        .put("buttonShown", button.isShown).put("buttonEnabled", button.isEnabled).put("buttonListener", button.hasOnClickListeners())
                        .put("buttonLabel", button.text.toString()).put("completed", saved?.completed).put("position", saved?.position)
                        .put("continueMovies", app.repo.continueWatching(pid, ItemKind.MOVIE).size).toString()
                }
                var before = ""
                try {
                    await {
                        val button = movieDetails.findViewById<Button>(R.id.movie_watched)
                        (movieDetails as androidx.appcompat.app.AppCompatActivity).lifecycle.currentState == androidx.lifecycle.Lifecycle.State.RESUMED &&
                            movieDetails.hasWindowFocus() && button.isShown && button.isEnabled && button.text.toString() == "Mark watched"
                    }
                    onMain { before = actionState(); movieDetails.findViewById<Button>(R.id.movie_watched).performClick() }
                    await { app.repo.watchProgress(movie)?.completed == true &&
                        movieDetails.findViewById<Button>(R.id.movie_watched).text.toString() == "Mark unwatched" }
                    await { app.repo.continueWatching(pid, ItemKind.MOVIE).isEmpty() }
                    onMain { movieDetails.findViewById<Button>(R.id.movie_clear).performClick() }
                    await { app.repo.resumePosition(movie) == 0L && app.repo.watchProgress(movie)?.completed == false &&
                        movieDetails.findViewById<Button>(R.id.movie_watched).text.toString() == "Mark watched" }
                } catch (failure: Throwable) {
                    var after = ""
                    onMain { after = actionState() }
                    capture("movie-watched-action-failure")
                    throw IllegalStateException("Watched action before=$before; after=$after", failure)
                }
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
            checkStep("visible episode cards have accurate partial green progress and no unwatched line") {
                val seasons = seriesDetails.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.series_seasons)
                onMain { seasons.scrollToPosition(0) }
                await { seasons.findViewHolderForAdapterPosition(0) != null }
                val rail = seasons.findViewHolderForAdapterPosition(0)!!.itemView
                    .findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.series_episode_rail)
                onMain { rail.scrollToPosition(0) }
                await {
                    val first = rail.findViewHolderForAdapterPosition(0)?.itemView
                    val second = rail.findViewHolderForAdapterPosition(1)?.itemView
                    if (first == null || second == null) false else {
                        val blank = first.findViewById<android.widget.ProgressBar>(R.id.episode_watch_progress)
                        val partial = second.findViewById<android.widget.ProgressBar>(R.id.episode_watch_progress)
                        blank.visibility == android.view.View.INVISIBLE && partial.visibility == android.view.View.VISIBLE &&
                            partial.progress in 410..420 && partial.progressTintList?.defaultColor == android.graphics.Color.parseColor("#FF43D17A")
                    }
                }
                capture("episode-green-progress")
            }
            checkStep("D-pad switches seasons and restores each rail selection") {
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
                await { seriesDetails.findViewById<TextView>(R.id.series_selected).text.contains("Season 2 · Episode 1") }
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_UP)
                await { seriesDetails.findViewById<TextView>(R.id.series_selected).text.contains("Season 1 · Episode 2") }
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_LEFT)
                await { seriesDetails.findViewById<TextView>(R.id.series_selected).text.contains("Season 1 · Episode 1") }
                await { seriesDetails.findViewById<TextView>(R.id.series_watch_status).text.contains("Last watched: S1 E2") }
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_RIGHT)
                await { seriesDetails.findViewById<TextView>(R.id.series_selected).text.contains("Season 1 · Episode 2") }
                capture("series-header-after-focus")
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
                fun backgroundDiagnostic(): String {
                    var value = ""
                    onMain {
                        val active = screen.findViewById<PlayerView>(R.id.player_view).player
                        value = JSONObject().put("lifecycle", (screen as androidx.appcompat.app.AppCompatActivity).lifecycle.currentState.name)
                            .put("focused", screen.hasWindowFocus()).put("finishing", screen.isFinishing).put("destroyed", screen.isDestroyed)
                            .put("playerPresent", active != null).put("playbackState", active?.playbackState)
                            .put("dialogPresent", dialog() != null).put("dialogShowing", dialog()?.isShowing == true)
                            .put("taskId", screen.taskId).put("topWindowPackage", uiAutomation.rootInActiveWindow?.packageName?.toString()).toString()
                    }
                    return value
                }
                try {
                    await { (screen as androidx.appcompat.app.AppCompatActivity).lifecycle.currentState <= androidx.lifecycle.Lifecycle.State.CREATED ||
                        screen.findViewById<PlayerView>(R.id.player_view).player == null }
                } catch (failure: Throwable) {
                    capture("background-stop-failure")
                    throw IllegalStateException("Background did not stop/dispose player: ${backgroundDiagnostic()}", failure)
                }
                val beforeForeground = backgroundDiagnostic()
                val manager = targetContext.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val task = manager.appTasks.first { it.taskInfo.taskId == taskId }
                onMain { task.moveToFront() }
                try { await { dialog()?.isShowing == true } }
                catch (failure: Throwable) {
                    capture("background-dialog-failure")
                    throw IllegalStateException("Before foreground=$beforeForeground; after=${backgroundDiagnostic()}", failure)
                }
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
            fun openEpisode(index: Int): Activity {
                val monitor = addMonitor(PlayerActivity::class.java.name, null, false)
                onMain { Nav.play(seriesDetails, queue, index, fromBeginning = true) }
                return try { (waitForMonitorWithTimeout(monitor, 10_000) ?: error("Episode player did not open")).also {
                    opened.add(it); ready(it)
                } } finally { removeMonitor(monitor) }
            }
            checkStep("completed episode card displays a full green progress line") {
                val seasons = seriesDetails.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.series_seasons)
                onMain { seasons.scrollToPosition(0) }
                await { seasons.findViewHolderForAdapterPosition(0) != null }
                val rail = seasons.findViewHolderForAdapterPosition(0)!!.itemView
                    .findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.series_episode_rail)
                onMain { rail.scrollToPosition(1) }
                await {
                    rail.findViewHolderForAdapterPosition(1)?.itemView?.findViewById<android.widget.ProgressBar>(R.id.episode_watch_progress)
                        ?.let { it.visibility == android.view.View.VISIBLE && it.progress == 1000 } == true
                }
                capture("episode-completed-green-progress")
            }
            checkStep("unfinished episode progress keeps priority over an earlier completed episode") {
                check(app.repo.watchProgress(queue[2])?.resume ?: 0 > 0)
                await { seriesDetails.findViewById<Button>(R.id.series_continue).text.toString() == "Resume S2 E1" &&
                    seriesDetails.findViewById<Button>(R.id.series_next).text.toString() == "Next: S2 E2" }
                capture("unfinished-episode-priority")
            }
            checkStep("autoplay OFF returns after natural end without launching another episode") {
                app.settings.autoPlayNextEpisode = false
                // Isolate completed-history navigation from the earlier partial S2E1 resume test.
                app.repo.markWatched(queue[0], true)
                app.repo.clearWatchProgress(queue[2])
                app.repo.clearWatchProgress(queue[3])
                val manual = openEpisode(1)
                onMain { player(manual).seekTo(player(manual).duration - 250); player(manual).play() }
                await { manual.isFinishing || manual.isDestroyed }
                check(app.repo.watchProgress(queue[1])?.completed == true)
            }
            checkStep("the series Next action names and opens the first episode after completed history without skipping it") {
                await { seriesDetails.findViewById<Button>(R.id.series_next).text.toString() == "Next: S2 E1" }
                val next = playClick(seriesDetails, R.id.series_next)
                ready(next)
                onMain { check(player(next).currentMediaItem?.mediaId == queue[2].url) }
                finishScreen(next)
            }
            checkStep("manual Next remains available while autoplay is OFF") {
                app.settings.autoPlayNextEpisode = false
                val manual = openEpisode(0)
                onMain {
                    val controls = manual.findViewById<PlayerView>(R.id.player_view).player!!
                    check(controls.hasNextMediaItem()); controls.seekToNextMediaItem()
                }
                await { player(manual).currentMediaItem?.mediaId == queue[1].url && player(manual).playbackState == Player.STATE_READY }
                finishScreen(manual)
            }
            checkStep("final episode completion returns without wrapping to the first episode") {
                app.settings.autoPlayNextEpisode = true
                val final = openEpisode(queue.lastIndex)
                onMain { check(!final.findViewById<PlayerView>(R.id.player_view).player!!.hasNextMediaItem())
                    player(final).seekTo(player(final).duration - 250); player(final).play() }
                await { final.isFinishing || final.isDestroyed }
                check(app.repo.watchProgress(queue.last())?.completed == true)
            }
            checkStep("guide catch-up opens the archive player without losing its URL or archive flag") {
                val now = System.currentTimeMillis()
                val live = movie.copy(kind = ItemKind.LIVE, remoteId = "1", name = "Synthetic archive source", catchupDays = 2)
                val program = Program("fixture", now - 120_000, now - 60_000, "Synthetic archive programme")
                val archive = app.repo.catchupItem(live, program, now) ?: error("Archive was unavailable")
                val playerMonitor = addMonitor(PlayerActivity::class.java.name, null, false)
                val movieMonitor = addMonitor(MovieDetailsActivity::class.java.name, null, false)
                try {
                    onMain { Nav.open(seriesDetails, archive, listOf(archive)) }
                    val archiveScreen = waitForMonitorWithTimeout(playerMonitor, 10_000) ?: error("Archive player did not open")
                    opened.add(archiveScreen)
                    check(movieMonitor.hits == 0)
                    check(YourIptvApp.playQueue.single().let { it.isCatchup && it.url == archive.url })
                    checkStep("archive playback decodes real frames from the exact catch-up URL") {
                        ready(archiveScreen)
                        await { player(archiveScreen).videoDecoderCounters?.let {
                            it.ensureUpdated(); it.renderedOutputBufferCount > 0 } == true }
                        onMain { check(player(archiveScreen).currentMediaItem?.mediaId == archive.url) }
                        capture("catchup-decoded-frame")
                    }
                    checkStep("catch-up account changes block stale archive replay on foreground and retry") {
                        val source = app.repo.playlist(pid)!!
                        try {
                            val taskId = archiveScreen.taskId
                            onMain { check(archiveScreen.moveTaskToBack(true)) }
                            SystemClock.sleep(700)
                            app.repo.savePlaylist(source.copy(password = "changed-fixture-account"))
                            val manager = targetContext.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                            val task = manager.appTasks.first { it.taskInfo.taskId == taskId }
                            onMain { task.moveToFront() }
                            val expected = "The playlist account changed. Reopen this programme from the guide."
                            await { archiveScreen.findViewById<TextView>(R.id.message).let {
                                it.visibility == android.view.View.VISIBLE && it.text.toString() == expected }
                            }
                            onMain { check(archiveScreen.findViewById<PlayerView>(R.id.player_view).player?.isPlaying != true) }
                            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_CENTER)
                            await { archiveScreen.findViewById<TextView>(R.id.message).text.toString() == expected }
                            onMain { check(archiveScreen.findViewById<PlayerView>(R.id.player_view).player?.isPlaying != true) }
                            check(app.repo.watchProgress(archive) == null)
                            capture("catchup-source-edit-blocked")
                        } finally { app.repo.savePlaylist(source) }
                    }
                    finishScreen(archiveScreen)
                } finally { removeMonitor(playerMonitor); removeMonitor(movieMonitor) }
            }
            finishScreen(seriesDetails)
            checkStep("Settings to Home to launcher resumes the remembered live source without a fullscreen marker") {
                check(!app.settings.settingsLockEnabled) { "The isolated fixture must have ordinary Settings access" }
                app.settings.resumeLastChannel = false
                val live = movie.copy(kind = ItemKind.LIVE, remoteId = "startup-live", name = "Synthetic remembered live",
                    url = movie.url + "?startup_live=1")
                Db(targetContext).use { it.writableDatabase.insertOrThrow("items", null, Db.itemValues(live)) }
                checkStep("a fresh guide-request Activity stays in the guide rather than resuming fullscreen live") {
                    app.settings.activePlaylistId = pid
                    app.settings.rememberLiveChannel(live)
                    app.settings.resumeLastChannel = true
                    val playerMonitor = addMonitor(PlayerActivity::class.java.name, null, false)
                    try {
                        val guide = startActivitySync(Intent(targetContext, MainActivity::class.java)
                            .setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(MainActivity.EXTRA_GUIDE_CHANNEL, live.url)
                            .putExtra(MainActivity.EXTRA_GUIDE_GROUP, live.group))
                        opened.add(guide)
                        await { guide.hasWindowFocus() }
                        SystemClock.sleep(1500)
                        onMain { check(guide.hasWindowFocus() && playerMonitor.hits == 0) }
                        capture("fresh-guide-request")
                        finishScreen(guide)
                    } finally { removeMonitor(playerMonitor) }
                    onMain { app.livePlayback.stop() }
                }
                app.settings.resumeLastChannel = false
                browsePid = app.repo.savePlaylist(Playlist(name = "VOD UI QA browsing ${System.nanoTime()}",
                    type = SourceType.M3U, url = "https://startup-fixture.invalid/browsing", refreshHours = 0))
                app.settings.activePlaylistId = browsePid
                val main = startActivitySync(Intent(targetContext, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                opened.add(main)
                await { main.hasWindowFocus() }
                val settingsMonitor = addMonitor(SettingsActivity::class.java.name, null, false)
                val settingsScreen = try {
                    onMain { main.startActivity(Intent(main, SettingsActivity::class.java)) }
                    waitForMonitorWithTimeout(settingsMonitor, 10_000) ?: error("Settings did not open")
                } finally { removeMonitor(settingsMonitor) }
                opened.add(settingsScreen)
                await { settingsScreen.hasWindowFocus() }
                app.settings.rememberLiveChannel(live)
                app.settings.resumeLastChannel = true
                check(!PlayerActivity.takeLeftWhileWatchingLive(targetContext))
                // Shell input uses the real HOME key. The next intent is what an Android launcher sends.
                uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").close()
                await { !settingsScreen.hasWindowFocus() }
                val playerMonitor = addMonitor(PlayerActivity::class.java.name, null, false)
                try {
                    uiAutomation.executeShellCommand("am start -W -n ${targetContext.packageName}/com.youriptv.app.ui.MainActivity " +
                        "-a android.intent.action.MAIN -c android.intent.category.LAUNCHER").use { output ->
                        java.io.FileInputStream(output.fileDescriptor).bufferedReader().use { it.readText() }
                    }
                    val resumed = waitForMonitorWithTimeout(playerMonitor, 15_000) ?: error("Launcher did not resume live playback")
                    opened.add(resumed); ready(resumed)
                    onMain { check(player(resumed).currentMediaItem?.mediaId == live.url) }
                    check(YourIptvApp.playQueue.all { it.playlistId == pid && it.kind == ItemKind.LIVE })
                    check(app.settings.activePlaylistId == pid)
                    capture("launcher-remembered-live")
                    finishScreen(resumed)
                } finally { removeMonitor(playerMonitor) }
                app.settings.resumeLastChannel = false
                finishScreen(main)
            }
            checkStep("main History groups series once beside individual movies and opens series progress") {
                app.settings.resumeLastChannel = false
                app.settings.activePlaylistId = pid
                app.repo.saveWatchProgress(movie, 1500, 6024)
                app.repo.saveWatchProgress(queue[0], 1250, 6024)
                app.repo.saveWatchProgress(queue[1], 6024, 6024, ended = true)
                val saved = (queue + movie).associate { it.url to app.repo.watchProgress(it) }
                val main = startActivitySync(Intent(targetContext, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                opened.add(main)
                await { main.hasWindowFocus() }
                fun nav(label: String) = onMain {
                    val container = main.findViewById<android.widget.LinearLayout>(R.id.nav_container)
                    (0 until container.childCount).map { container.getChildAt(it) }.filterIsInstance<TextView>()
                        .single { it.text.toString() == label }.performClick()
                }
                val items = main.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.items)
                nav("History")
                await { main.findViewById<android.view.View>(R.id.state_container).visibility != android.view.View.VISIBLE &&
                    (items.adapter as? ItemAdapter)?.items?.let { rows ->
                    rows.count { it.kind == ItemKind.SERIES && it.url == series.url } == 1 &&
                        rows.count { it.kind == ItemKind.MOVIE && it.url == movie.url } == 1 &&
                        rows.none { it.kind == ItemKind.EPISODE } } == true }
                capture("main-grouped-history")
                nav("Shows")
                val groups = main.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.groups)
                await { (groups.adapter?.itemCount ?: 0) >= 3 }
                onMain { groups.scrollToPosition(2) }
                await { groups.findViewHolderForAdapterPosition(2) != null }
                onMain { groups.findViewHolderForAdapterPosition(2)!!.itemView.performClick() }
                await { main.findViewById<android.view.View>(R.id.state_container).visibility != android.view.View.VISIBLE &&
                    (items.adapter as? ItemAdapter)?.let {
                    it.posterMode && it.items.size == 1 && it.items.single().kind == ItemKind.SERIES &&
                        it.items.single().url == series.url } == true }
                onMain { items.scrollToPosition(0) }
                await { items.findViewHolderForAdapterPosition(0) != null }
                capture("series-history-one-parent-poster")
                val seriesMonitor = addMonitor(SeriesActivity::class.java.name, null, false)
                try {
                    onMain { items.findViewHolderForAdapterPosition(0)!!.itemView.performClick() }
                    val reopened = waitForMonitorWithTimeout(seriesMonitor, 10_000) ?: error("History parent did not open series details")
                    opened.add(reopened)
                    await { reopened.findViewById<Button>(R.id.series_continue).text.toString() == "Resume S1 E1" }
                    capture("history-parent-series-progress")
                    finishScreen(reopened)
                } finally { removeMonitor(seriesMonitor) }
                check((queue + movie).all { app.repo.watchProgress(it) == saved[it.url] })
                finishScreen(main)
            }
            val progressBeforeRefresh = app.repo.watchProgress(episode)
            app.repo.refresh(app.repo.playlist(pid)!!, {})
            checkStep("catalogue refresh preserves watch progress") { check(app.repo.watchProgress(episode) == progressBeforeRefresh) }
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
            failed++; results.put(JSONObject().put("name", "UI fixture execution").put("passed", false)
                .put("type", e.javaClass.simpleName).put("message", e.message.orEmpty().take(400))
                .put("stack", e.stackTrace.take(8).joinToString("\n")))
        } finally {
            onMain { opened.asReversed().filter { !it.isDestroyed }.forEach { it.finish() }
                app.livePlayback.stop(); YourIptvApp.playQueue = oldQueue }
            waitForIdleSync()
            if (pid != 0L) app.repo.deletePlaylist(pid)
            if (browsePid != 0L) app.repo.deletePlaylist(browsePid)
            val editor = preferences.edit()
            oldPreferences.forEach { (key, value) -> when (value) {
                null -> editor.remove(key)
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Long -> editor.putLong(key, value)
            } }
            check(editor.commit())
        }
        finish(if (failed == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
            putString("results", results.toString()); putInt("failures", failed)
        })
    }
}
