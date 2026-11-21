package com.youriptv.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.EditText
import androidx.media3.common.util.Log
import com.youriptv.app.data.SourceType
import com.youriptv.app.ui.PlaylistEditActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Exercises Save and load on the real editor. Never targets the installed production application. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SourceEditorInstrumentation : Instrumentation() {
    private fun main(action: () -> Unit) { runOnMainSync(action) }
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        if (targetContext.packageName != "com.youriptv.app.playtest") { finish(Activity.RESULT_CANCELED, Bundle()); return }
        val input = File(targetContext.filesDir, "source-editor.private.json")
        waitForIdleSync()
        val rows = JSONArray()
        var failures = 0
        var screen: Activity? = null
        val app = YourIptvApp.instance
        val obsolete = app.repo.playlists().filter { it.name.startsWith("Editor validation ") }
        val previous = app.settings.activePlaylistId.takeUnless { value -> obsolete.any { it.id == value } } ?: 0L
        obsolete.forEach { app.repo.deletePlaylist(it.id) }
        app.settings.activePlaylistId = previous
        val previousIds = app.repo.playlists().map { it.id }.toSet()
        val created = arrayListOf<Long>()
        val oldLog = Log.getLogLevel()
        Log.setLogLevel(Log.LOG_LEVEL_OFF)
        try {
            val cfg = JSONObject(input.readText()); check(input.delete())
            check(!app.settings.settingsLockEnabled)
            for (type in listOf(SourceType.XTREAM, SourceType.M3U)) {
                val name = "Editor validation " + type.name + SystemClock.elapsedRealtime()
                val row = JSONObject().put("type", type.name)
                try {
                    val base = cfg.getString("base")
                    val user = cfg.getString("username"); val pass = cfg.getString("password")
                    val source = if (type == SourceType.XTREAM) base else base + "/get.php?username=" +
                        java.net.URLEncoder.encode(user, "UTF-8") + "&password=" + java.net.URLEncoder.encode(pass, "UTF-8") + "&type=m3u_plus&output=ts"
                    val activity = startActivitySync(Intent(targetContext, PlaylistEditActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    screen = activity; SystemClock.sleep(200)
                    main {
                        activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        activity.findViewById<Button>(if (type == SourceType.XTREAM) R.id.type_xtream else R.id.type_m3u).performClick()
                        activity.findViewById<EditText>(R.id.f_name).setText(name)
                        activity.findViewById<EditText>(R.id.f_url).setText(source)
                        activity.findViewById<EditText>(R.id.f_user).setText(user)
                        activity.findViewById<EditText>(R.id.f_pass).setText(pass)
                        activity.findViewById<Button>(R.id.btn_save).performClick()
                    }
                    if (type == SourceType.M3U) {
                        val until = SystemClock.elapsedRealtime() + 10_000
                        var clicked = false
                        while (!clicked && SystemClock.elapsedRealtime() < until) {
                            val nodes = uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Keep as M3U playlist").orEmpty()
                            val node = nodes.firstOrNull()
                            if (node != null) {
                                val rect = android.graphics.Rect(); node.getBoundsInScreen(rect)
                                val down = SystemClock.uptimeMillis()
                                for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                                    val event = android.view.MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                                        rect.centerX().toFloat(), rect.centerY().toFloat(), 0)
                                    event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                                    sendPointerSync(event); event.recycle()
                                }
                                clicked = true
                            }
                            if (!clicked) SystemClock.sleep(200)
                        }
                        check(clicked)
                    }
                    val until = SystemClock.elapsedRealtime() + 240_000
                    var loaded = false
                    while (!loaded && SystemClock.elapsedRealtime() < until) {
                        val value = app.repo.playlists().singleOrNull { it.name == name }
                        loaded = value != null && value.lastUpdated > 0 && value.liveCount > 0 &&
                            app.refreshCoordinator.states.value[value.id]?.busy == false
                        if (!loaded) SystemClock.sleep(300)
                    }
                    val playlist = app.repo.playlists().single { it.name == name }
                    created.add(playlist.id)
                    check(loaded && playlist.liveCount > 0 && playlist.url == source)
                    row.put("passed", true).put("live", playlist.liveCount).put("movies", playlist.movieCount)
                        .put("series", playlist.seriesCount).put("httpPreserved", playlist.url.startsWith("http://"))
                } catch (error: Throwable) { failures++; row.put("passed", false).put("errorType", error.javaClass.simpleName) }
                main { screen?.finish() }; SystemClock.sleep(200)
                rows.put(row)
            }
        } catch (error: Throwable) { failures++; rows.put(JSONObject().put("passed", false).put("errorType", error.javaClass.simpleName)) }
        finally {
            main { screen?.finish() }; SystemClock.sleep(200); input.delete()
            for (item in app.repo.playlists().filter { it.id !in previousIds && it.name.startsWith("Editor validation ") }) if (item.id !in created) created.add(item.id)
            created.forEach { app.repo.deletePlaylist(it) }
            app.settings.activePlaylistId = previous
            Log.setLogLevel(oldLog)
            File(targetContext.filesDir, "source-editor-result.json").writeText(JSONObject().put("results", rows).put("failures", failures)
                .put("privateInputDeleted", !input.exists()).put("previousActiveRestored", app.settings.activePlaylistId == previous).toString(2))
            finish(if (failures == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply { putInt("failures", failures) })
        }
    }
}
