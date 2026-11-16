package com.youriptv.app.ui

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import com.youriptv.app.R

/** Offline policy: readable on TVs without launching an external browser. */
class PrivacyActivity : RemoteActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        findViewById<TextView>(R.id.about_title).text = "Privacy policy"
        findViewById<TextView>(R.id.about_version).text = "Google Play distribution · November 16, 2026"
        findViewById<TextView>(R.id.about_body).text = getString(R.string.play_privacy_policy)
        findViewById<android.view.View>(R.id.about_privacy).visibility = android.view.View.GONE
        findViewById<ScrollView>(R.id.about_scroll).requestFocus()
    }
}
