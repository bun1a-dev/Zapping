package com.iwo.zapping.player

import com.iwo.zapping.data.BrowserTransport
import com.iwo.zapping.data.Endpoints
import com.iwo.zapping.data.ServiceApi
import com.iwo.zapping.data.Session
import com.iwo.zapping.data.SessionStore
import com.iwo.zapping.data.StreamRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Łączy sesję z odtwarzaczem: bilet na strumień, nagłówki, start i zamknięcie sesji
 * odtwarzania po stronie usługi. Przy szybkim przełączaniu kanałów wygrywa ostatnie żądanie.
 */
class PlaybackCoordinator(
    private val session: Session,
    private val api: ServiceApi,
    private val store: SessionStore,
    private val transport: BrowserTransport,
    private val player: PlayerController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var generation = 0

    suspend fun start(request: StreamRequest) {
        val mine = ++generation
        val ticket = session.authorized {
            store.activeChannelId?.let { runCatching { api.releaseStream(it) } }
            api.streamTicket(request)
        }
        if (mine != generation) return          // w międzyczasie wybrano inny kanał
        store.activeChannelId = request.channelId

        val headers = PlaybackHeaders(
            userAgent = transport.userAgent,
            origin = Endpoints.ORIGIN,
            licenseHeaders = mapOf(
                "User-Agent" to transport.userAgent,
                "Origin" to Endpoints.ORIGIN,
                "Referer" to Endpoints.HOME_URL,
                "Cookie" to transport.cookies(),
            ),
        )
        player.play(DrmSourceFactory.dash(ticket.manifestUrl, ticket.licenseUrl, headers))
    }

    /** Zatrzymuje obraz od razu, a sesję po stronie usługi zamyka w tle. */
    fun stop() {
        generation++
        player.stop()
        val channelId = store.activeChannelId ?: return
        store.activeChannelId = null
        scope.launch { runCatching { session.authorized { api.releaseStream(channelId) } } }
    }

    fun close() = scope.cancel()
}
