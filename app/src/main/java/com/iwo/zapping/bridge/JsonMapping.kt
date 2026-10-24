package com.iwo.zapping.bridge

import com.iwo.zapping.data.Channel
import com.iwo.zapping.data.Program
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
