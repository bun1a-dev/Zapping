package com.bun1a.zapping.bridge

import com.bun1a.zapping.AppGraph
import com.bun1a.zapping.data.Credentials
import com.bun1a.zapping.data.StreamRequest
import com.bun1a.zapping.player.PlaybackCoordinator
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/** Cała lista metod, które interfejs może wywołać. Każda robi jedną rzecz i deleguje dalej. */
fun registerRoutes(bridge: UiBridge, graph: AppGraph, playback: PlaybackCoordinator) {
    val session = graph.session
    val api = graph.api
    val transport = graph.transport

    bridge.register("session.state") { _ ->
        JSONObject().put("hasCredentials", session.hasCredentials)
    }

    bridge.register("session.login") { params ->
        session.signIn(Credentials(params.getString("login").trim(), params.getString("pin").trim()))
    }

    bridge.register("session.logout") { _ ->
        playback.stop()
        session.signOut()
    }

    bridge.register("channels") { _ ->
        session.authorized { api.channels() }.toJsonArray()
    }

    bridge.register("epg") { params ->
        val dayStartSec = startOfDaySec(daysAgo = params.optInt("day", 0))
        session.authorized { api.guide(dayStartSec) }.toJsonObject()
    }

    bridge.register("regions") { params ->
        session.authorized { api.regions(params.getString("channelId")) }.toRegionsJson()
    }

    bridge.register("play") { params ->
        playback.start(
            StreamRequest(
                channelId = params.getString("channelId"),
                programId = params.optString("programId").ifEmpty { null },
                startSec = if (params.has("start")) params.getLong("start") else null,
                endSec = if (params.has("end")) params.getLong("end") else null,
                region = params.optString("region").ifEmpty { null },
            ),
        )
    }

    bridge.register("stop") { _ -> playback.stop() }

    bridge.register("cache.clear") { _ -> transport.clearCache() }
}

private fun startOfDaySec(daysAgo: Int): Long =
    LocalDate.now().minusDays(daysAgo.toLong()).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
