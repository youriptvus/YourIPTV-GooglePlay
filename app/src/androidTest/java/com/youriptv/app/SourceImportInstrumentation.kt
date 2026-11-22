package com.youriptv.app

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.security.NetworkSecurityPolicy
import androidx.media3.common.util.Log
import com.youriptv.app.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Authorized account stays in an isolated, app-private input; reports contain no source identifiers. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SourceImportInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        if (targetContext.packageName != "com.youriptv.app.playtest") {
            finish(Activity.RESULT_CANCELED, Bundle()); return
        }
        val results = JSONArray()
        val created = arrayListOf<Long>()
        val input = File(targetContext.filesDir, "source-import.private.json")
        val logLevel = Log.getLogLevel()
        var failures = 0
        Log.setLogLevel(Log.LOG_LEVEL_OFF)
        try {
            waitForIdleSync()
            val config = JSONObject(input.readText())
            check(input.delete())
            val base = config.getString("base")
            val user = config.getString("username")
            val pass = config.getString("password")
            check(SecureTransport.isNetworkUrl(base))
            check(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(java.net.URI(base).host))
            val repo = YourIptvApp.instance.repo
            for (type in listOf(SourceType.XTREAM, SourceType.M3U)) {
                val started = android.os.SystemClock.elapsedRealtime()
                val row = JSONObject().put("type", type.name)
                try {
                    val url = if (type == SourceType.XTREAM) base else
                        base + "/get.php?username=" + java.net.URLEncoder.encode(user, "UTF-8") +
                            "&password=" + java.net.URLEncoder.encode(pass, "UTF-8") + "&type=m3u_plus&output=ts"
                    val id = repo.savePlaylist(Playlist(name = "Source import validation " + type.name,
                        type = type, url = url, username = if (type == SourceType.XTREAM) user else "",
                        password = if (type == SourceType.XTREAM) pass else "", refreshHours = 0))
                    created.add(id)
                    repo.refresh(repo.playlist(id)!!, {})
                    val loaded = repo.playlist(id)!!
                    check(loaded.liveCount > 0)
                    check(loaded.url == url)
                    row.put("live", loaded.liveCount).put("movies", loaded.movieCount)
                        .put("series", loaded.seriesCount).put("lastUpdated", loaded.lastUpdated)
                    if (type == SourceType.XTREAM) {
                        val guide = repo.refreshEpg(loaded, 24, 1, {})
                        row.put("guidePrograms", guide)
                        check(guide > 0)
                    }
                    row.put("passed", true)
                } catch (error: Throwable) {
                    failures++
                    row.put("passed", false).put("errorType", error.javaClass.simpleName)
                        .put("causeType", error.cause?.javaClass?.simpleName.orEmpty())
                }
                row.put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started)
                results.put(row)
            }
        } catch (error: Throwable) {
            failures++
            results.put(JSONObject().put("passed", false).put("errorType", error.javaClass.simpleName))
        } finally {
            input.delete()
            for (id in created) YourIptvApp.instance.repo.deletePlaylist(id)
            Log.setLogLevel(logLevel)
            val report = JSONObject().put("results", results).put("failures", failures)
                .put("privateInputDeleted", !input.exists()).put("createdPlaylistsRemoved", created.all { YourIptvApp.instance.repo.playlist(it) == null })
            File(targetContext.filesDir, "source-import-result.json").writeText(report.toString(2))
            finish(if (failures == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                Bundle().apply { putInt("failures", failures); putInt("checks", results.length()) })
        }
    }
}
