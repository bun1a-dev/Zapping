package com.bun1a.zapping.bridge

import com.bun1a.zapping.data.Channel
import com.bun1a.zapping.data.Program
import com.bun1a.zapping.data.Region
import org.json.JSONArray
import org.json.JSONObject

/** Kształt danych dla interfejsu. Czasy w milisekundach, bo tak liczy JavaScript. */
internal fun Channel.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("number", number)
    .put("name", name)
    .put("category", category)
    .put("logoUrl", logoUrl)
    .put("catchupDays", catchupDays)
    .put("isRegional", isRegional)

internal fun Program.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("start", startSec * 1000)
    .put("end", endSec * 1000)

internal fun List<Channel>.toJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it.toJson()) }
}

internal fun Map<String, List<Program>>.toJsonObject(): JSONObject = JSONObject().also { result ->
    forEach { (channelId, programs) ->
        result.put(channelId, JSONArray().also { array -> programs.forEach { array.put(it.toJson()) } })
    }
}

/** Interfejs potrzebuje tylko nazw. Sufiks adresu zostaje po stronie Kotlina. */
internal fun List<Region>.toRegionsJson(): JSONArray = JSONArray().also { array ->
    forEach { array.put(JSONObject().put("name", it.name)) }
}
