package com.youriptv.app.ui

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.youriptv.app.data.InsecureTransportException
import com.youriptv.app.data.SecureTransport

/** Enforces transport policy for every resource, including nested HLS playlists and segments. */
@OptIn(UnstableApi::class)
internal class SecureDataSourceFactory(
    private val delegate: DataSource.Factory,
    private val allowLoopbackHttp: Boolean,
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        val source = delegate.createDataSource()
        return object : DataSource by source {
            override fun open(dataSpec: DataSpec): Long {
                if (!SecureTransport.canLoadMedia(dataSpec.uri.toString(), allowLoopbackHttp))
                    throw InsecureTransportException(SecureTransport.HTTPS_REQUIRED)
                return source.open(dataSpec)
            }
        }
    }
}
