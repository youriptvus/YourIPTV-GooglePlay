package com.youriptv.app.data

import java.io.IOException
import java.net.URI
import java.net.URL

/** The Play distribution accepts HTTPS network sources without rewriting saved credentials. */
object SecureTransport {
    const val HTTPS_REQUIRED = "HTTPS is required. Ask your provider for a secure HTTPS address."
    const val SECURE_REDIRECT_REQUIRED = "An insecure provider redirect was blocked. Ask your provider for an HTTPS address."
    private val scheme = Regex("^([A-Za-z][A-Za-z0-9+.-]*):")

    fun isHttpsUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", true) && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    fun requireHttps(value: String) {
        if (!isHttpsUrl(value)) throw InsecureTransportException(HTTPS_REQUIRED)
    }

    fun redirect(from: String, location: String): String {
        val previous = URL(from)
        val next = URL(previous, location)
        if (previous.protocol.equals("https", true) && !next.protocol.equals("https", true))
            throw InsecureTransportException(SECURE_REDIRECT_REQUIRED)
        return next.toString()
    }

    private fun schemeOf(value: String): String? = scheme.find(value.trimStart())?.groupValues?.get(1)?.lowercase()

    fun canHandOffMedia(value: String): Boolean = isHttpsUrl(value) || schemeOf(value) in setOf("file", "content")

    fun canLoadMedia(value: String, allowLoopbackHttp: Boolean = false): Boolean = when (schemeOf(value)) {
        "https" -> isHttpsUrl(value)
        "http" -> allowLoopbackHttp && runCatching {
            val uri = URI(value)
            uri.host == "127.0.0.1" && uri.rawUserInfo == null && uri.port in -1..65535
        }.getOrDefault(false)
        "file", "content", "asset", "android.resource", "rawresource", "data" -> true
        null -> value.isNotBlank() && !value.trimStart().startsWith("//") && value.none { it.code < 32 }
        else -> false
    }
}

class InsecureTransportException(message: String) : IOException(message)
