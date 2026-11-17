package com.youriptv.app

import android.app.Activity
import android.app.Instrumentation
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.security.NetworkSecurityPolicy
import android.view.WindowManager
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
import com.youriptv.app.ui.MainActivity
import java.io.File
import java.net.URI
import java.net.InetAddress
import java.net.ServerSocket
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
    private var subsetServer: ServerSocket? = null

    /** In-process, loopback-only metadata; real media remains a direct provider connection. */
    private fun serveSubset(text: String): String {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        subsetServer = server
        val path = "/$runId/catalog.m3u"
        val bytes = text.toByteArray(Charsets.UTF_8)
        requireTrial(bytes.size in 1..64 * 1024, "subset_size_invalid")
        Thread({
            try {
                server.accept().use { socket ->
                    socket.soTimeout = 10_000
                    val reader = socket.getInputStream().bufferedReader(Charsets.US_ASCII)
                    val request = reader.readLine().orEmpty()
                    while (!reader.readLine().isNullOrEmpty()) { /* Headers stay in memory only. */ }
                    val output = socket.getOutputStream()
                    if (request.startsWith("GET $path ")) {
                        output.write(("HTTP/1.1 200 OK\r\nContent-Type: audio/x-mpegurl\r\n" +
                            "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
                        output.write(bytes)
                    } else output.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII))
                    output.flush()
                }
            } catch (_: Throwable) { /* No raw socket diagnostics or private payload logging. */ }
        }, "private-subset-metadata").apply { isDaemon = true; start() }
        return "http://127.0.0.1:${server.localPort}$path"
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        runId = arguments?.getString("runId").orEmpty()
        start()
    }

    private class TrialFailure(val code: String) : RuntimeException()
    private fun requireTrial(condition: Boolean, code: String) { if (!condition) throw TrialFailure(code) }
    /** Classify in memory only; exception text can contain credential URLs and is never emitted. */
    private fun safeReason(error: Throwable): String {
        val text = error.message?.lowercase().orEmpty()
        return when {
            "cleartext" in text || "not permitted" in text -> "cleartext_blocked"
            "timed out" in text || "timeout" in text -> "network_timeout"
            "unexpected end" in text || "eof" in text -> "truncated_response"
            "no space" in text || "enospc" in text -> "storage_full"
            "too many redirects" in text -> "redirect_limit"
            "connection reset" in text || "stream was reset" in text -> "connection_interrupted"
            "permission denied" in text || "eacces" in text -> "permission_denied"
            "enoent" in text || "no such file" in text -> "staging_path_missing"
            "certificate" in text || "ssl" in text -> "tls_failed"
            else -> "operation_failed"
        }
    }
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
                .put("reasonCode", (error as? TrialFailure)?.code ?: safeReason(error))
                .put("causeTypes", JSONArray(generateSequence(error.cause) { it.cause }.take(5).map { it.javaClass.simpleName }.toList())))
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
        var previousResumeLast = true
        var hadActiveKey = false
        var hadLastKey = false
        var hadResumeKey = false
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
            hadResumeKey = prefs.contains("resume_last")
            previousPlaylist = currentApp.settings.activePlaylistId
            previousLastChannel = currentApp.settings.lastChannelUrl
            previousResumeLast = currentApp.settings.resumeLastChannel
            previousQueue = YourIptvApp.playQueue
            report.put("runId", runId).put("scope", "Private trial in isolated test package; decoder evidence, no screenshots or raw responses")
            val config = JSONObject(input.readText(Charsets.UTF_8))
            requireTrial(input.delete(), "private_input_cleanup_failed")
            requireTrial(config.optString("nonce") == runId, "private_input_nonce_mismatch")
            val playlistUrl = config.getString("playlistUrl")
            val epgUrl = config.getString("epgUrl")
            requireTrial(URI(playlistUrl).scheme in setOf("http", "https"), "unsupported_playlist_transport")
            val host = URI(playlistUrl).host ?: throw TrialFailure("missing_provider_host")
            val cleartextAllowed = if (Build.VERSION.SDK_INT >= 24)
                NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(host)
            else NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted
            report.put("cleartextAllowedForConfiguredHost", cleartextAllowed)
                .put("cacheUsableMiB", targetContext.cacheDir.usableSpace / (1024 * 1024))
                .put("stagingDirectoryPresent", File(targetContext.cacheDir, "refresh-stage").isDirectory)
            val account = config.getJSONObject("account")
            requireTrial(account.optBoolean("authenticated") && account.optString("status") == "active", "account_not_active")
            requireTrial(account.optInt("activeConnections", -1) == 0, "account_already_in_use")
            val observeSeconds = config.optInt("observeSeconds", 60)
            requireTrial(observeSeconds in 20..180, "observation_out_of_bounds")
            report.put("account", JSONObject().put("authenticated", true).put("status", "active")
                .put("maximumConnections", account.optInt("maximumConnections", 0))
                .put("activeConnectionsBefore", 0))
            // Model an ordinary foreground import without starting a remembered source.
            currentApp.settings.resumeLastChannel = false
            screen = startActivitySync(Intent(targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            onMain { screen!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            waitForIdleSync()
            val power = targetContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val keyguard = targetContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            onMain {
                report.put("foregroundWindowShownBeforeImport", screen!!.window.decorView.isShown)
                    .put("foregroundWindowFocusBeforeImport", screen!!.hasWindowFocus())
                    .put("deviceInteractiveBeforeImport", power.isInteractive)
                    .put("keyguardLockedBeforeImport", keyguard.isKeyguardLocked)
            }
            val trialName = "Private validation ${runId.take(8)}"
            val subset = config.optString("subsetM3u")
            val bounded = subset.isNotEmpty()
            val catalogSource = if (bounded) serveSubset(subset) else playlistUrl
            report.put("catalogScope", if (bounded) "bounded_private_live_subset" else "full_provider_catalog")
                .put("mediaNetworkPath", "direct_provider_connection")
            val imported = checkStep("actual_m3u_plus_import") {
                playlistId = currentApp.repo.savePlaylist(Playlist(name = trialName, type = SourceType.M3U,
                    url = catalogSource, epgUrl = if (bounded) "" else epgUrl, refreshHours = 0))
                requireTrial(playlistId > 0, "playlist_creation_failed")
                report.put("createdPlaylistId", playlistId)
                val playlist = currentApp.repo.playlist(playlistId) ?: throw TrialFailure("playlist_missing")
                currentApp.repo.refresh(playlist, { progress ->
                    report.put("catalogPhase", when {
                        progress.startsWith("Checking the provider") -> "archive_metadata"
                        progress.startsWith("Downloading playlist") -> "playlist_download"
                        progress.startsWith("Loaded ") -> "playlist_parse"
                        else -> "catalog_processing"
                    })
                })
                subsetServer?.close()
                subsetServer = null
                val refreshed = currentApp.repo.playlist(playlistId) ?: throw TrialFailure("playlist_missing")
                report.put("catalogCounts", JSONObject().put("live", refreshed.liveCount)
                    .put("movies", refreshed.movieCount).put("seriesOrEpisodes", refreshed.seriesCount))
                report.put("applicationEffectiveConnectionLimit", currentApp.repo.connectionLimit(playlistId))
                requireTrial(refreshed.liveCount > 0, "live_catalog_empty")
            }
            if (imported) {
                if (!bounded) checkStep("actual_xmltv_import") {
                    val playlist = currentApp.repo.playlist(playlistId) ?: throw TrialFailure("playlist_missing")
                    val count = currentApp.repo.refreshEpg(playlist, currentApp.settings.epgPastHours, currentApp.settings.epgFutureDays, {})
                    report.put("importedPrograms", count)
                    requireTrial(count > 0, "guide_programs_missing")
                }
                else report.put("guideScope", "Not repeated for subset playback; full provider guide verified separately on phone")
                val channels = currentApp.repo.items(playlistId, ItemKind.LIVE, null)
                val candidates = channels.filter {
                    !it.isCatchup && !it.isLocal && !AdultGroups.isAdult(it.group) && !AdultGroups.isAdult(it.name) &&
                        !currentApp.settings.isGroupHidden(it.group) && !currentApp.settings.isGroupLocked(it.group) &&
                        URI(it.url).scheme in setOf("http", "https") &&
                        !URI(it.url).path.orEmpty().endsWith(".m3u8", ignoreCase = true)
                }.take(16).sortedByDescending { currentApp.repo.nowAndNext(playlistId, it.tvgId).isNotEmpty() }.take(3)
                report.put("guideMappedLiveSampleCount", channels.take(128).count { currentApp.repo.nowAndNext(playlistId, it.tvgId).isNotEmpty() })
                checkStep("actual_live_render_and_observation") {
                    requireTrial(candidates.isNotEmpty(), "no_unrestricted_live_candidate")
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
            try { subsetServer?.close() } catch (_: Throwable) { }
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
                if (hadResumeKey) editor.putBoolean("resume_last", previousResumeLast) else editor.remove("resume_last")
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
        var container = "unknown"
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
                val containerFormat = player?.videoFormat?.containerMimeType?.lowercase().orEmpty()
                container = if (containerFormat in setOf("video/mp2t", "video/mp4", "application/x-mpegurl", "application/vnd.apple.mpegurl")) containerFormat else "unknown"
                ready = rendered > 0 && width > 0 && height > 0 && player?.isPlaying == true
            }
            if (ready) break
            SystemClock.sleep(200)
        }
        result.put("rendered", ready).put("videoWidth", width).put("videoHeight", height).put("videoMimeType", mime)
            .put("containerMimeType", container).put("containerEvidenceAvailable", container != "unknown")
            .put("mpegTsContainerConfirmed", container == "video/mp2t")
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
