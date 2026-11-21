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
            val attempt = runCatching(block)
            val passed = attempt.getOrDefault(false)
            if (!passed) failures++
            results.put(JSONObject().put("name", name).put("passed", passed).apply {
                attempt.exceptionOrNull()?.let { error -> put("type", error.javaClass.simpleName)
                    put("message", error.message.orEmpty().take(400)); put("stack", error.stackTrace.take(8).joinToString("\n")) }
            })
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
                helper.writableDatabase.insertOrThrow("items", null, Db.itemValues(movie.copy(kind = ItemKind.SERIES,
                    name = "Fixture series", logo = "fixture-poster", remoteId = "2", url = ep.seriesUrl)))
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
            val ep2 = ep.copy(remoteId = "22", url = "https://vod-fixture.invalid/series/22.mp4", episode = 2)
            val otherEpisode = ep.copy(remoteId = "31", url = "https://vod-fixture.invalid/series/31.mp4",
                seriesKey = "other-series", seriesUrl = "https://vod-fixture.invalid/series/3", seriesRemoteId = "3")
            val movie2 = movie.copy(remoteId = "9", name = "Second fixture movie", url = "https://vod-fixture.invalid/movie/9.mp4")
            Db(targetContext).use { helper ->
                helper.writableDatabase.insertOrThrow("items", null, Db.itemValues(movie2))
                helper.writableDatabase.insertOrThrow("items", null, Db.itemValues(movie.copy(kind = ItemKind.SERIES,
                    name = "Fixture series", remoteId = "3", url = otherEpisode.seriesUrl)))
            }
            repo.saveWatchProgress(movie2, 25_000, 120_000)
            repo.saveWatchProgress(ep, 180_000, 180_000, ended = true)
            android.os.SystemClock.sleep(3)
            repo.saveWatchProgress(otherEpisode, 20_000, 180_000)
            android.os.SystemClock.sleep(3)
            repo.saveWatchProgress(ep2, 75_000, 180_000)
            repo.close(); repo = Repository(targetContext)
            verify("main history collapses completed and partial episodes under one catalogue parent") {
                val rows = repo.history(id).filter { it.kind == ItemKind.SERIES && it.remoteId == "2" }
                rows.size == 1 && rows.single().url == ep.seriesUrl && rows.single().name == "Fixture series" &&
                    rows.single().logo == "fixture-poster" && rows.single().season == 2 && rows.single().episode == 2
            }
            verify("main history retains separate movies and same-title shows with distinct identities") {
                val rows = repo.groupedHistory(id)
                rows.count { it.kind == ItemKind.MOVIE } == 2 &&
                    rows.filter { it.kind == ItemKind.SERIES }.map { it.remoteId }.toSet() == setOf("2", "3")
            }
            verify("history limit follows grouping and sorts by most recently watched episode") {
                val first = repo.history(id, 1).single()
                val two = repo.history(id, 2)
                first.kind == ItemKind.SERIES && first.remoteId == "2" && two.size == 2 &&
                    two[1].kind == ItemKind.SERIES && two[1].remoteId == "3"
            }
            verify("history grouping preserves each episode position and completion after restart") {
                repo.watchProgress(ep)?.completed == true && repo.resumePosition(ep2) == 75_000L &&
                    repo.resumePosition(otherEpisode) == 20_000L && repo.resumePosition(movie2) == 25_000L
            }
            repo.saveWatchProgress(ep, 55_000, 180_000)
            repo.clearWatchProgress(movie2)
            repo.close(); repo = Repository(targetContext)
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
            verify("account edits do not leak grouped history from the previous account") { repo.history(id).isEmpty() }
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
                val legacy1 = old.copy(kind = ItemKind.EPISODE, name = "Legacy show S01 E01",
                    url = "https://vod-fixture.invalid/legacy/episode-one.mp4")
                val legacy2 = legacy1.copy(name = "Legacy show S01 E02", url = "https://vod-fixture.invalid/legacy/episode-two.mp4")
                repo.recordHistory(legacy1, 45_000, 180_000)
                android.os.SystemClock.sleep(3)
                repo.recordHistory(legacy2, 65_000, 180_000)
                verify("legacy M3U episode history groups without deleting original playback positions") {
                    val entry = repo.history(m3uId).single { it.kind == ItemKind.SERIES }
                    entry.url == SeriesNames.pseudoUrl("QA", "Legacy show") && entry.name == "Legacy show" &&
                        entry.season == 1 && entry.episode == 2 && repo.resumePosition(legacy1) == 45_000L &&
                        repo.resumePosition(legacy2) == 65_000L
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
        } catch (error: Throwable) {
            failures++; results.put(JSONObject().put("name", "fixture execution").put("passed", false)
                .put("type", error.javaClass.simpleName).put("message", error.message.orEmpty().take(400))
                .put("stack", error.stackTrace.take(8).joinToString("\n")))
        } finally {
            source?.let { repo.deletePlaylist(it.id) }
            repo.close()
        }
        finish(if (failures == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
            putString("results", results.toString()); putInt("failures", failures)
        })
    }
}
