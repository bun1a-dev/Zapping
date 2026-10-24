package com.iwo.zapping.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Endpointy usługi i nic więcej: bez stanu, bez logiki sesji.
 * Odświeżaniem i ponownym logowaniem zajmuje się [Session].
 */
class ServiceApi(
    private val transport: BrowserTransport,
    private val identity: () -> DeviceIdentity?,
) {

    // ---------- konto ----------

    /** Zwraca token odświeżania. PIN idzie czystym tekstem po HTTPS, tak jak w przeglądarce. */
    suspend fun login(credentials: Credentials): String {
        val body = JSONObject().put("login", credentials.login).put("password", credentials.pin)
        val result = call(
            "POST", "auth/login",
            query = "deviceType=${Endpoints.DEVICE_TYPE}",
            body = body,
            accept = "application/json;ver=2",
            authenticated = false,
        )
        if (result.status != 200 || transport.cookie("Token") == null) {
            throw LoginRejectedException(
                if (result.status == 401) "Nieprawidłowy login lub PIN."
                else "Logowanie nie powiodło się (HTTP ${result.status}).",
            )
        }
        return json(result.body).optString("refreshToken")
    }

    suspend fun household(): DeviceIdentity {
        val result = call("GET", "core/household/household")
        val data = json(result.body)
        if (!data.has("householdExtId")) {
            throw RequestFailedException(result.status, "Nie udało się pobrać danych konta.")
        }
        return DeviceIdentity(data.getString("householdExtId"), data.optString("hhTech"))
    }

    /** Zwraca nowy token odświeżania albo null, gdy usługa go odrzuciła. */
    suspend fun refresh(refreshToken: String): String? {
        val result = call(
            "POST", "auth/refresh",
            body = JSONObject().put("refreshToken", refreshToken),
            authenticated = false,
        )
        return if (result.status == 200) json(result.body).optString("refreshToken") else null
    }

    suspend fun logout() {
        call("GET", "auth/logout", authenticated = false)
    }

    // ---------- kanały i program ----------

    suspend fun channels(): List<Channel> {
        val list = json(call("GET", "live/channel/all").body).optJSONArray("channelList")
            ?: throw RequestFailedException(0, "Usługa nie zwróciła listy kanałów.")
        return list.objects().mapNotNull { parseChannel(it) }
    }

    /** Program z doby zaczynającej się o [dayStartSec] i z doby poprzedniej (tak odpowiada usługa). */
    suspend fun guide(dayStartSec: Long): Map<String, List<Program>> {
        val result = linkedMapOf<String, MutableList<Program>>()
        for (start in listOf(dayStartSec - SECONDS_PER_DAY, dayStartSec)) {
            val response = call("GET", "core/tvguide/epg", query = "date=$start")
            val entries = json(response.body).optJSONArray("guide") ?: continue
            for (entry in entries.objects()) {
                val channelId = entry.optString("channelExtId")
                val programs = entry.optJSONArray("programs")
                if (channelId.isEmpty() || programs == null) continue
                result.getOrPut(channelId) { mutableListOf() }
                    .addAll(programs.objects().map { parseProgram(it) })
            }
        }
        return result
    }

    // ---------- odtwarzanie ----------

    suspend fun streamTicket(request: StreamRequest): StreamTicket {
        val path = buildString {
            append("live/channel/").append(request.channelId)
            if (request.isCatchup) append("/").append(request.programId).append("/catchup")
        }

        var result = call("GET", path)
        if (result.status == 412) {            // urządzenie wymaga rejestracji
            registerTerminal()
            result = call("GET", path)
        }

        val data = json(result.body)
        val casToken = data.optString("casToken")
        if (casToken.isEmpty()) {
            throw StreamUnavailableException("Ten kanał jest teraz niedostępny (HTTP ${result.status}).")
        }

        var manifestUrl = data.optString("streamUrl")
        if (request.isCatchup && request.startSec != null && request.endSec != null) {
            manifestUrl += "&begin=${request.startSec}&end=${request.endSec}"
        }
        return StreamTicket(manifestUrl, "${Endpoints.LICENSE}?token=${encode(casToken)}")
    }

    suspend fun releaseStream(channelId: String) {
        call("DELETE", "live/channel/$channelId/session")
    }

    private suspend fun registerTerminal() {
        val household = identity()?.householdId.orEmpty()
        val body = JSONObject()
            .put("deviceManufacture", "web")
            .put("deviceModel", "web")
            .put("deviceType", Endpoints.DEVICE_TYPE)
            .put("name", "FIREFOX")
            .put("serialNumber", Endpoints.TERMINAL_SERIAL_PREFIX + household)
        val result = call("POST", "core/device/register", body = body)
        if (result.status != 200) {
            throw StreamUnavailableException("Nie udało się zarejestrować urządzenia.")
        }
    }

    // ---------- wspólne ----------

    /** [authenticated] = true: status 401 oznacza wygasłą sesję. */
    private suspend fun call(
        method: String,
        path: String,
        query: String? = null,
        body: JSONObject? = null,
        accept: String = "application/json;ver=1",
        authenticated: Boolean = true,
    ): HttpResult {
        val url = buildString {
            append(Endpoints.API).append(path)
            append("?deviceCat=").append(Endpoints.DEVICE_CATEGORY)
            if (query != null) append("&").append(query)
        }
        val headers = linkedMapOf("Accept" to accept, "OtvDeviceInfo2" to deviceInfoHeader())
        if (body != null) headers["Content-Type"] = "application/json;ver=1"

        val result = transport.request(method, url, headers, body?.toString())
        if (authenticated && result.status == 401) throw UnauthorizedException()
        return result
    }

    private fun deviceInfoHeader(): String {
        val id = identity()
        return listOf(
            "dev_type" to Endpoints.DEVICE_TYPE,
            "app_version" to Endpoints.APP_VERSION,
            "web_native" to transport.userAgent,
            "hh_tech" to id?.hhTech.orEmpty(),
            "serial_number" to (id?.let { Endpoints.TERMINAL_SERIAL_PREFIX + it.householdId } ?: ""),
            "offer" to "",
        ).joinToString("&") { (key, value) -> "$key=${encode(value)}" }
    }

    private fun parseChannel(item: JSONObject): Channel? {
        if (!item.optBoolean("isSubscribed")) return null
        val features = item.optJSONObject("playFeatures")?.optJSONObject("otg") ?: return null
        if (!features.optBoolean("isChannelAllowed")) return null

        val catchupDays = if (features.optBoolean("isCatchUp")) {
            (item.optLong("catchupDuration") / SECONDS_PER_DAY).toInt().coerceAtLeast(1)
        } else {
            0
        }
        val logo = item.optString("logoSignature")
        return Channel(
            id = item.getString("channelExtId"),
            number = item.optInt("channelNumber"),
            name = item.getString("name"),
            category = item.optString("category").ifEmpty { "Inne" },
            logoUrl = if (logo.isEmpty()) "" else Endpoints.IMAGES + logo,
            catchupDays = catchupDays,
        )
    }

    private fun parseProgram(item: JSONObject) = Program(
        id = item.optString("programExtId"),
        title = item.optString("name"),
        startSec = item.optLong("startTimeUtc"),
        endSec = item.optLong("endTimeUtc"),
    )

    private fun json(text: String): JSONObject = runCatching { JSONObject(text) }.getOrDefault(JSONObject())

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
    }
}
