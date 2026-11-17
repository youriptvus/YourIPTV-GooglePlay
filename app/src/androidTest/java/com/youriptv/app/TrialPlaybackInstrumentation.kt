package com.youriptv.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.youriptv.app.data.AdultGroups
import com.youriptv.app.data.ItemKind
import com.youriptv.app.data.MediaItem
import com.youriptv.app.data.Playlist
import com.youriptv.app.data.SourceType
import com.youriptv.app.ui.PlayerActivity
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONArray
import org.json.JSONObject

/** Explicit private-input path. No URLs, titles, screenshots, or exception messages are emitted. */
@OptIn(UnstableApi::class)
class TrialPlaybackInstrumentation : Instrumentation() {
    private var runId = ""
    private val results = JSONArray()
    private val attempts = JSONArray()
    private var failures = 0
    private var playlistId = 0L
    private var screen: Activity? = null
    private var report = JSONObject()

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        runId = arguments?.getString("runId").orEmpty()
        start()
    }

    private class TrialFailure(val code: String) : RuntimeException()
    private fun requireTrial(condition: Boolean, code: String) { if (!condition) throw TrialFailure(code) }
    /** An assertion on Android's main looper must fail this test, not crash the app process. */
    private fun onMain(action: () -> Unit) {
        val failure = AtomicReference<Throwable?>()
        runOnMainSync {
            try { action() } catch (error: Throwable) { failure.set(error) }
        }
        failure.get()?.let { throw it }
    }
    private fun checkStep(name: String, action: () -> Unit): Boolean {
        val started = SystemClock.elapsedRealtime()
        return try {
            action()
            results.put(JSONObject().put("check", name).put("passed", true)
                .put("elapsedMs", SystemClock.elapsedRealtime() - started))
            true
        } catch (error: Throwable) {
            failures++
            results.put(JSONObject().put("check", name).put("passed", false)
                .put("elapsedMs", SystemClock.elapsedRealtime() - started)
                .put("errorType", error.javaClass.simpleName)
                .put("reasonCode", (error as? TrialFailure)?.code ?: "operation_failed"))
            false
        }
    }

    override fun onStart() {
        if (targetContext.packageName != "com.youriptv.app.playtest" || !runId.matches(Regex("[0-9a-f]{32}"))) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("reasonCode", "isolated_target_or_nonce_required") })
            return
        }
        val input = File(targetContext.filesDir, "trial-input.json")
        var app: YourIptvApp? = null
        var previousQueue: List<MediaItem> = emptyList()
        var previousPlaylist = 0L
        var previousLastChannel = ""
        var hadActiveKey = false
        var hadLastKey = false
        val previousLogLevel = Log.getLogLevel()
        Log.setLogLevel(Log.LOG_LEVEL_OFF)
        val started = SystemClock.elapsedRealtime()
        try {
            waitForIdleSync()
            val currentApp = YourIptvApp.instance
            app = currentApp
            val prefs = targetContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            hadActiveKey = prefs.contains("active_playlist")
            hadLastKey = prefs.contains("last_channel")
            previousPlaylist = currentApp.settings.activePlaylistId
            previousLastChannel = currentApp.settings.lastChannelUrl
            previousQueue = YourIptvApp.playQueue
            report.put("runId", runId).put("scope", "Private trial in isolated test package; decoder evidence, no screenshots or raw responses")
            val config = JSONObject(input.readText(Charsets.UTF_8))
            requireTrial(input.delete(), "private_input_cleanup_failed")
            requireTrial(config.optString("nonce") == runId, "private_input_nonce_mismatch")
            val playlistUrl = config.getString("playlistUrl")
            val epgUrl = config.getString("epgUrl")
            requireTrial(URI(playlistUrl).scheme in setOf("http", "https"), "unsupported_playlist_transport")
            val account = config.getJSONObject("account")
            requireTrial(account.optBoolean("authenticated") && account.optString("status") == "active", "account_not_active")
            requireTrial(account.optInt("activeConnections", -1) == 0, "account_already_in_use")
            val observeSeconds = config.optInt("observeSeconds", 60)
            requireTrial(observeSeconds in 20..180, "observation_out_of_bounds")
            report.put("account", JSONObject().put("authenticated", true).put("status", "active")
                .put("maximumConnections", account.optInt("maximumConnections", 0))
                .put("activeConnectionsBefore", 0))
            val trialName = "Private validation ${runId.take(8)}"
            val imported = checkStep("actual_m3u_plus_import") {
                playlistId = currentApp.repo.savePlaylist(Playlist(name = trialName, type = SourceType.M3U,
                    url = playlistUrl, epgUrl = epgUrl, refreshHours = 0))
                requireTrial(playlistId > 0, "playlist_creation_failed")
                report.put("createdPlaylistId", playlistId)
                val playlist = currentApp.repo.playlist(playlistId) ?: throw TrialFailure("playlist_missing")
                currentApp.repo.refresh(playlist, {})
                val refreshed = currentApp.repo.playlist(playlistId) ?: throw TrialFailure("playlist_missing")
                report.put("catalogCounts", JSONObject().put("live", refreshed.liveCount)
                    .put("movies", refreshed.movieCount).put("seriesOrEpisodes", refreshed.seriesCount))
                report.put("applicationEffectiveConnectionLimit", currentApp.repo.connectionLimit(playlistId))
                requireTrial(refreshed.liveCount > 0, "live_catalog_empty")
            }
            if (imported) {
                checkStep("actual_xmltv_import") {
                    val playlist = currentApp.repo.playlist(playlistId) ?: throw TrialFailure("playlist_missing")
                    val count = currentApp.repo.refreshEpg(playlist, currentApp.settings.epgPastHours, currentApp.settings.epgFutureDays, {})
                    report.put("importedPrograms", count)
                    requireTrial(count > 0, "guide_programs_missing")
                }
                val channels = currentApp.repo.items(playlistId, ItemKind.LIVE, null)
                val candidates = channels.filter {
                    !it.isCatchup && !it.isLocal && !AdultGroups.isAdult(it.group) && !AdultGroups.isAdult(it.name) &&
                        !currentApp.settings.isGroupHidden(it.group) && !currentApp.settings.isGroupLocked(it.group) &&
                        it.url.substringBefore('?').endsWith(".ts", ignoreCase = true)
                }.take(16).sortedByDescending { currentApp.repo.nowAndNext(playlistId, it.tvgId).isNotEmpty() }.take(3)
                report.put("guideMappedLiveSampleCount", channels.take(128).count { currentApp.repo.nowAndNext(playlistId, it.tvgId).isNotEmpty() })
                checkStep("actual_live_mpegts_render_and_observation") {
                    requireTrial(candidates.isNotEmpty(), "no_unrestricted_mpegts_candidate")
                    var succeeded = false
                    for ((index, item) in candidates.withIndex()) {
                        if (play(item, observeSeconds, index + 1)) { succeeded = true; break }
                        onMain { screen?.finish(); currentApp.livePlayback.stop() }
                        waitForIdleSync()
                        SystemClock.sleep(5_000)
                    }
                    requireTrial(succeeded, "no_candidate_completed_playback")
                }
            }
        } catch (error: Throwable) {
            failures++
            results.put(JSONObject().put("check", "trial_prerequisites").put("passed", false)
                .put("errorType", error.javaClass.simpleName)
                .put("reasonCode", (error as? TrialFailure)?.code ?: "operation_failed"))
        } finally {
            input.delete()
            if (app != null) checkStep("trial_only_cleanup_and_previous_preferences_restored") {
                val initialized = app ?: throw TrialFailure("application_unavailable")
                onMain {
                    screen?.finish()
                    initialized.livePlayback.stop()
                    YourIptvApp.playQueue = previousQueue
                }
                waitForIdleSync()
                if (playlistId > 0) {
                    val created = initialized.repo.playlist(playlistId)
                    requireTrial(created == null || created.name == "Private validation ${runId.take(8)}", "trial_identity_changed")
                    if (created != null) initialized.repo.deletePlaylist(playlistId)
                    requireTrial(initialized.repo.playlist(playlistId) == null, "trial_playlist_cleanup_failed")
                }
                val editor = targetContext.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                if (hadActiveKey) editor.putLong("active_playlist", previousPlaylist) else editor.remove("active_playlist")
                if (hadLastKey) editor.putString("last_channel", previousLastChannel) else editor.remove("last_channel")
                requireTrial(editor.commit(), "previous_preferences_restore_failed")
                report.put("privateInputDeleted", !input.exists()).put("createdTrialPlaylistRemoved", playlistId > 0)
                    .put("previousPreferencesRestored", true).put("playerReleased", initialized.livePlayback.player == null)
            }
            Log.setLogLevel(previousLogLevel)
            report.put("runId", runId).put("checks", results).put("attempts", attempts).put("failures", failures)
                .put("elapsedMs", SystemClock.elapsedRealtime() - started)
            try { File(targetContext.filesDir, "trial-result.json").writeText(report.toString(2), Charsets.UTF_8) }
            catch (_: Throwable) { failures++ }
            finish(if (failures == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
                putString("runId", runId); putInt("failures", failures); putInt("checks", results.length())
            })
        }
    }

    private fun play(item: MediaItem, observeSeconds: Int, ordinal: Int): Boolean {
        val app = YourIptvApp.instance
        val errors = AtomicInteger()
        val firstFrames = AtomicInteger()
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrames.incrementAndGet() }
            override fun onPlayerError(error: PlaybackException) { errors.incrementAndGet() }
        }
        val result = JSONObject().put("candidateOrdinal", ordinal).put("streamTransport", URI(item.url).scheme)
        attempts.put(result)
        onMain { app.livePlayback.stop(); YourIptvApp.playQueue = listOf(item) }
        screen = startActivitySync(Intent(targetContext, PlayerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        onMain { app.livePlayback.player?.addListener(listener) }
        var rendered = 0
        var width = 0
        var height = 0
        var mime = "unknown"
        var ready = false
        val deadline = SystemClock.elapsedRealtime() + 45_000
        while (SystemClock.elapsedRealtime() < deadline) {
            onMain {
                val player = app.livePlayback.player
                player?.videoDecoderCounters?.ensureUpdated()
                rendered = player?.videoDecoderCounters?.renderedOutputBufferCount ?: 0
                width = player?.videoFormat?.width ?: 0
                height = player?.videoFormat?.height ?: 0
                val format = player?.videoFormat?.sampleMimeType.orEmpty()
                mime = if (format in setOf("video/avc", "video/hevc", "video/mpeg2", "video/mp4v-es", "video/av01", "video/x-vnd.on2.vp9")) format else "other"
                ready = rendered > 0 && width > 0 && height > 0 && player?.isPlaying == true
            }
            if (ready) break
            SystemClock.sleep(200)
        }
        result.put("rendered", ready).put("videoWidth", width).put("videoHeight", height).put("videoMimeType", mime)
            .put("renderedFirstFrameEvents", firstFrames.get()).put("decoderRenderedBuffersAtStart", rendered)
        if (!ready) {
            result.put("playerErrorEvents", errors.get()).put("passed", false)
            onMain { app.livePlayback.player?.removeListener(listener) }
            return false
        }
        val before = rendered
        val started = SystemClock.elapsedRealtime()
        var playingSamples = 0
        var samples = 0
        var lastPlaying = started
        var maximumGap = 0L
        while (SystemClock.elapsedRealtime() - started < observeSeconds * 1_000L) {
            var playing = false
            onMain {
                val player = app.livePlayback.player
                player?.videoDecoderCounters?.ensureUpdated()
                rendered = player?.videoDecoderCounters?.renderedOutputBufferCount ?: 0
                playing = player?.isPlaying == true && screen?.isFinishing == false
            }
            val now = SystemClock.elapsedRealtime()
            if (playing) { playingSamples++; lastPlaying = now }
            maximumGap = maxOf(maximumGap, now - lastPlaying)
            samples++
            SystemClock.sleep(500)
        }
        val passed = rendered > before && playingSamples * 2 >= samples && errors.get() == 0
        result.put("actualObservationMs", SystemClock.elapsedRealtime() - started).put("playingSamples", playingSamples)
            .put("samples", samples).put("maximumObservedNotPlayingMs", maximumGap)
            .put("decoderRenderedBufferDelta", rendered - before).put("playerErrorEvents", errors.get()).put("passed", passed)
        onMain { app.livePlayback.player?.removeListener(listener) }
        return passed
    }
}
