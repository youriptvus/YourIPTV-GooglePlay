package com.youriptv.app

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.youriptv.app.data.*
import org.json.JSONArray
import org.json.JSONObject

/** Native SQLite and durable-writer checks; never runs on or clears a customer installation. */
class VodProgressInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        if (targetContext.packageName != "com.youriptv.app.playtest") {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("error", "Isolated package required") }); return
        }
        waitForIdleSync()
        val results = JSONArray()
        var repo = Repository(targetContext)
        var source: Playlist? = null
        var failures = 0
        fun verify(name: String, block: () -> Boolean) {
            val passed = runCatching(block).getOrDefault(false)
            if (!passed) failures++
            results.put(JSONObject().put("name", name).put("passed", passed))
        }
        try {
            val id = repo.savePlaylist(Playlist(name = "VOD QA ${System.nanoTime()}", type = SourceType.XTREAM,
                url = "https://vod-fixture.invalid", username = "fixture", refreshHours = 0))
            source = repo.playlist(id)!!
            val movie = MediaItem(playlistId = id, kind = ItemKind.MOVIE, name = "Fixture movie", group = "QA",
                url = "https://vod-fixture.invalid/movie/8.mp4", remoteId = "8")
            val ep = movie.copy(kind = ItemKind.EPISODE, remoteId = "21", url = "https://vod-fixture.invalid/series/21.mp4",
                seriesKey = "fixture-series", seriesName = "Fixture series", seriesUrl = "https://vod-fixture.invalid/series/2", seriesRemoteId = "2", season = 2, episode = 1)
            Db(targetContext).use { helper ->
                helper.writableDatabase.insertOrThrow("items", null, Db.itemValues(movie))
                helper.writableDatabase.insertOrThrow("items", null, Db.itemValues(movie.copy(kind = ItemKind.SERIES, remoteId = "2", url = ep.seriesUrl)))
            }
            repo.saveWatchProgress(movie, 45_000, 120_000)
            verify("pending position immediately visible") { repo.resumePosition(movie) == 45_000L }
            repo.saveWatchProgress(ep, 55_000, 180_000)
            repo.close(); repo = Repository(targetContext)
            verify("position persists after repository restart") { repo.resumePosition(movie) == 45_000L }
            verify("exact episode and parent persist") {
                val saved = repo.continueWatching(id, ItemKind.SERIES).single()
                saved.season == 2 && saved.episode == 1 && saved.remoteId == "21" && saved.seriesRemoteId == "2"
            }
            verify("movie continues once") { repo.continueWatching(id, ItemKind.MOVIE).size == 1 }
            val rotated = movie.copy(url = "https://cdn-fixture.invalid/new/8.mkv", name = "Changed title", id = 999)
            verify("provider id survives changed catalogue row and URL") { repo.resumePosition(rotated) == 45_000L }
            repo.saveWatchProgress(movie, 119_500, 120_000)
            verify("stopped credits remain unfinished") { repo.watchProgress(movie)?.completed == false && repo.resumePosition(movie) == 119_500L }
            repo.saveWatchProgress(movie, 120_000, 120_000, ended = true)
            verify("natural completion marks watched") { repo.watchProgress(movie)?.completed == true && repo.resumePosition(movie) == 0L }
            verify("completed movie leaves continue watching") { repo.continueWatching(id, ItemKind.MOVIE).isEmpty() }
            repo.markWatched(movie, false)
            verify("mark unwatched clears resume") { repo.watchProgress(movie)?.completed == false && repo.resumePosition(movie) == 0L }
            repeat(20) { repo.saveWatchProgress(movie, it * 1000L, 120_000) }
            repo.close(); repo = Repository(targetContext)
            verify("ordered writes preserve latest snapshot") { repo.resumePosition(movie) == 19_000L }
            repo.clearWatchProgress(movie)
            repo.close(); repo = Repository(targetContext)
            verify("individual reset persists and does not revive legacy history") { repo.resumePosition(movie) == 0L && repo.resumePosition(ep) == 55_000L }
            repo.saveWatchProgress(movie.copy(kind = ItemKind.LIVE), 30_000, 120_000)
            verify("live does not enter VOD history") { repo.watchProgress(movie.copy(kind = ItemKind.LIVE)) == null }
            repo.savePlaylist(source!!.copy(username = "another-account"))
            verify("account edits isolate progress") { repo.resumePosition(ep) == 0L }
            repo.saveWatchProgress(ep.copy(watchScope = WatchPolicy.scope(source!!)), 90_000, 180_000)
            verify("old playback cannot save into an edited account") { repo.resumePosition(ep) == 0L }
            repo.savePlaylist(source!!)
            verify("original account progress retained") { repo.resumePosition(ep) == 55_000L }
            val m3uId = repo.savePlaylist(Playlist(name = "Signed M3U QA", type = SourceType.M3U,
                url = "https://vod-fixture.invalid/get.php?username=fixture&password=fixture", refreshHours = 0))
            try {
                val old = movie.copy(playlistId = m3uId, remoteId = "", url = "https://vod-fixture.invalid/video?id=8&token=old")
                val fresh = old.copy(url = "https://vod-fixture.invalid/video?id=8&token=new")
                Db(targetContext).use { it.writableDatabase.insertOrThrow("items", null, Db.itemValues(fresh)) }
                repo.saveWatchProgress(old, 45_000, 120_000)
                verify("M3U Continue Watching resolves refreshed signed URLs by stable content identity") {
                    repo.continueWatching(m3uId, ItemKind.MOVIE).single().url == fresh.url && repo.resumePosition(fresh) == 45_000L
                }
            } finally { repo.deletePlaylist(m3uId) }
            val acquired = java.util.concurrent.CountDownLatch(1)
            val release = java.util.concurrent.CountDownLatch(1)
            val blocker = Thread {
                Db(targetContext).use { helper ->
                    val db = helper.writableDatabase
                    db.beginTransaction()
                    try { acquired.countDown(); release.await(3, java.util.concurrent.TimeUnit.SECONDS) }
                    finally { db.endTransaction() }
                }
            }.apply { start() }
            check(acquired.await(5, java.util.concurrent.TimeUnit.SECONDS))
            runOnMainSync { repo.saveWatchProgress(movie, 40_000, 120_000) }
            android.os.SystemClock.sleep(100)
            val before = android.os.SystemClock.elapsedRealtime()
            runOnMainSync { repo.saveWatchProgress(movie, 41_000, 120_000) }
            val saveMs = android.os.SystemClock.elapsedRealtime() - before
            release.countDown(); blocker.join(5_000)
            verify("catalogue writer does not block player progress snapshots") { saveMs < 500 && repo.resumePosition(movie) == 41_000L }
            repo.clearHistory(id)
            repo.close(); repo = Repository(targetContext)
            verify("clear history drains pending writes") { repo.continueWatching(id, ItemKind.SERIES).isEmpty() && repo.resumePosition(ep) == 0L }
        } catch (_: Throwable) {
            failures++; results.put(JSONObject().put("name", "fixture execution").put("passed", false))
        } finally {
            source?.let { repo.deletePlaylist(it.id) }
            repo.close()
        }
        finish(if (failures == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
            putString("results", results.toString()); putInt("failures", failures)
        })
    }
}
