package com.youriptv.app.ui

import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.youriptv.app.R

class AboutActivity : RemoteActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        findViewById<TextView>(R.id.about_title).text = "About ${Branding.displayName(this)}"
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()
        findViewById<TextView>(R.id.about_version).text = "Version $version · Android ${Build.VERSION.RELEASE} · ${Build.MODEL}"
        findViewById<TextView>(R.id.about_body).text = Branding.aboutBody(this)
        findViewById<Button>(R.id.about_privacy).setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }
        findViewById<ScrollView>(R.id.about_scroll).requestFocus()
    }

}
