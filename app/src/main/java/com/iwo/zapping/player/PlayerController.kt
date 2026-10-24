package com.iwo.zapping.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import org.json.JSONObject

/**
 * Właściciel ExoPlayera. Wszystkie metody wołamy z wątku głównego.
 * Zdarzenia dla interfejsu: "player.state" i "player.progress".
 */
@OptIn(UnstableApi::class)
class PlayerController(
    private val context: Context,
    private val emit: (event: String, payload: JSONObject) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var view: PlayerView? = null

    private val progressTicker = object : Runnable {
        override fun run() {
            val current = player ?: return
            emit("player.progress", JSONObject().put("positionMs", current.currentPosition))
            main.postDelayed(this, PROGRESS_INTERVAL_MS)
        }
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> emitState("loading")
                Player.STATE_READY -> emitState("playing")
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Błąd odtwarzania: ${error.errorCodeName}", error)
            emitState("error", describe(error))
        }
    }

    fun attach(playerView: PlayerView) {
        view = playerView
        player?.let { playerView.player = it }
    }

    fun play(source: MediaSource) {
        val exo = player ?: create()
        exo.setMediaSource(source)
        exo.prepare()
        exo.playWhenReady = true
        main.removeCallbacks(progressTicker)
        main.post(progressTicker)
        emitState("loading")
    }

    fun stop() {
        main.removeCallbacks(progressTicker)
        player?.stop()
        player?.clearMediaItems()
    }

    fun release() {
        main.removeCallbacks(progressTicker)
        view?.player = null
        player?.release()
        player = null
    }

    private fun create(): ExoPlayer {
        val selector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(true))
        }
        return ExoPlayer.Builder(context)
            .setTrackSelector(selector)
            .build()
            .also {
                it.addListener(listener)
                player = it
                view?.player = it
            }
    }

    private fun emitState(state: String, message: String? = null) {
        val payload = JSONObject().put("state", state)
        if (message != null) payload.put("message", message)
        emit("player.state", payload)
    }

    private fun describe(error: PlaybackException): String = when (error.errorCode) {
        in PlaybackException.ERROR_CODE_DRM_UNSPECIFIED..PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED ->
            "Nie udało się uzyskać licencji odtwarzania."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "Problem z połączeniem. Sprawdź internet."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            "Usługa odrzuciła żądanie strumienia."
        else -> "Nie udało się odtworzyć (${error.errorCodeName})."
    }

    private companion object {
        const val TAG = "Zapping"
        const val PROGRESS_INTERVAL_MS = 1_000L
    }
}
