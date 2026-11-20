package com.youriptv.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Bundle
import com.youriptv.app.data.PinCheck
import com.youriptv.app.data.PinKind
import com.youriptv.app.data.Settings
import com.youriptv.app.data.SettingsLockPolicy
import org.json.JSONArray
import org.json.JSONObject

/** Only generated preferences are used. Never reads, clears or changes the user's settings. */
class SettingsDefaultsInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        if (targetContext.packageName != "com.youriptv.app.playtest") {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("error", "Isolated package required") }); return
        }
        waitForIdleSync()
        val results = JSONArray()
        val names = mutableListOf<String>()
        val prefix = "settings_defaults_qa_${System.nanoTime()}_"
        var failures = 0
        fun fixture(label: String): Context {
            val name = prefix + label
            names.add(name)
            return object : ContextWrapper(targetContext) {
                override fun getApplicationContext(): Context = this
                override fun getSharedPreferences(ignored: String, mode: Int): SharedPreferences =
                    targetContext.getSharedPreferences(name, mode)
            }
        }
        fun checkStep(name: String, action: () -> Unit) {
            try { action(); results.put(JSONObject().put("name", name).put("passed", true)) }
            catch (error: Throwable) { failures++; results.put(JSONObject().put("name", name)
                .put("passed", false).put("type", error.javaClass.simpleName)) }
        }
        fun removeChoice(context: Context, removePlayInitialization: Boolean = true) {
            val edit = context.getSharedPreferences("settings", 0).edit().remove(SettingsLockPolicy.LOCAL_KEY)
            if (removePlayInitialization) edit.remove(SettingsLockPolicy.PLAY_INITIALIZED_KEY)
            check(edit.commit())
        }
        try {
            checkStep("fresh default is OFF and remains OFF after Settings reconstruction") {
                val c = fixture("fresh")
                check(!Settings(c).settingsLockEnabled)
                check(c.getSharedPreferences("settings", 0).contains(SettingsLockPolicy.LOCAL_KEY))
                check(!Settings(c).settingsLockEnabled)
            }
            checkStep("unrelated existing preferences do not enable the Settings lock") {
                val c = fixture("existing")
                c.getSharedPreferences("settings", 0).edit().putBoolean("resume_last", true).commit()
                check(!Settings(c).settingsLockEnabled)
            }
            checkStep("previous explicit ON survives initialization") {
                val c = fixture("on")
                c.getSharedPreferences("settings", 0).edit().putBoolean(SettingsLockPolicy.LOCAL_KEY, true).commit()
                check(Settings(c).settingsLockEnabled)
            }
            checkStep("previous explicit OFF wins even when a Settings PIN exists") {
                val c = fixture("off")
                val s = Settings(c); s.pins.set(PinKind.SETTINGS, "6042")
                check(!Settings(c).settingsLockEnabled)
                check(Settings(c).pins.checkSettingsAccess("6042") == PinCheck.Ok)
            }
            checkStep("existing Settings PIN keeps a missing-choice upgrade protected") {
                val c = fixture("pin")
                Settings(c).pins.set(PinKind.SETTINGS, "6042"); removeChoice(c)
                val s = Settings(c)
                check(s.settingsLockEnabled)
                check(s.pins.checkSettingsAccess("6042") == PinCheck.Ok)
            }
            checkStep("legacy Settings restriction and its parental PIN are preserved") {
                val c = fixture("legacy")
                Settings(c).pins.set(PinKind.PARENTAL, "3951"); removeChoice(c)
                c.getSharedPreferences("settings", 0).edit().putBoolean("lock_settings", true).commit()
                val s = Settings(c)
                check(s.settingsLockEnabled)
                check(s.pins.check(PinKind.PARENTAL, "3951") == PinCheck.Ok)
            }
            checkStep("parental PIN alone stays independent of the Settings default") {
                val c = fixture("parental")
                Settings(c).pins.set(PinKind.PARENTAL, "3951"); removeChoice(c)
                val s = Settings(c)
                check(!s.settingsLockEnabled)
                check(s.pins.check(PinKind.PARENTAL, "3951") == PinCheck.Ok)
            }
            checkStep("Admin PIN remains protected while ordinary Settings default OFF") {
                val c = fixture("admin")
                Settings(c).pins.setAdmin("738164"); removeChoice(c)
                val s = Settings(c)
                check(!s.settingsLockEnabled)
                check(s.pins.checkSettingsMaster("738164") == PinCheck.Ok)
            }
            checkStep("existing Settings PIN protects missing local state after Play initialization") {
                val c = fixture("restored")
                Settings(c).pins.set(PinKind.SETTINGS, "6042"); removeChoice(c, false)
                check(Settings(c).settingsLockEnabled)
            }
        } finally {
            names.forEach { name ->
                check(name.startsWith(prefix))
                targetContext.deleteSharedPreferences(name)
            }
        }
        finish(if (failures == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
            putString("results", results.toString()); putInt("failures", failures)
        })
    }
}
