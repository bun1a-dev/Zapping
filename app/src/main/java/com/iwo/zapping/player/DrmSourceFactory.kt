package com.iwo.zapping.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.source.MediaSource

/** Nagłówki, które usługa wymaga przy pobieraniu strumienia i licencji. */
data class PlaybackHeaders(
    val userAgent: String,
    val origin: String,
    val licenseHeaders: Map<String, String>,
)

/** Buduje źródło DASH zabezpieczone Widevine. Nie zna usługi, dostaje gotowe adresy. */
@OptIn(UnstableApi::class)
internal object DrmSourceFactory {

    fun dash(manifestUrl: String, licenseUrl: String, headers: PlaybackHeaders): MediaSource {
        val licenseDataSource = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers.licenseHeaders)
        val drmManager = DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .build(HttpMediaDrmCallback(licenseUrl, licenseDataSource))

        val streamDataSource = DefaultHttpDataSource.Factory()
            .setUserAgent(headers.userAgent)
            .setDefaultRequestProperties(mapOf("Origin" to headers.origin))

        val mediaItem = MediaItem.Builder()
            .setUri(manifestUrl)
            .setMimeType(MimeTypes.APPLICATION_MPD)
            .build()

        return DashMediaSource.Factory(streamDataSource)
            .setDrmSessionManagerProvider { drmManager }
            .createMediaSource(mediaItem)
    }
}
