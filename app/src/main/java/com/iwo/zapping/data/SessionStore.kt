package com.iwo.zapping.data

import android.content.Context

/**
 * Wszystko, co zostaje na urządzeniu między uruchomieniami: dane logowania,
 * token odświeżania i metadane sesji. Sekrety trafiają do pamięci tylko zaszyfrowane.
 */
class SessionStore(context: Context, private val secrets: SecretBox = SecretBox()) {

    private val prefs = context.getSharedPreferences("zapping_session", Context.MODE_PRIVATE)

    var credentials: Credentials?
        get() {
            val login = readSecret(KEY_LOGIN) ?: return null
            val pin = readSecret(KEY_PIN) ?: return null
            return Credentials(login, pin)
        }
        set(value) {
            writeSecret(KEY_LOGIN, value?.login)
            writeSecret(KEY_PIN, value?.pin)
        }

    var refreshToken: String?
        get() = readSecret(KEY_REFRESH)
        set(value) = writeSecret(KEY_REFRESH, value)

    var tokenTimeSec: Long
        get() = prefs.getLong(KEY_TOKEN_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_TOKEN_TIME, value).apply()

    var identity: DeviceIdentity?
        get() {
            val household = prefs.getString(KEY_HOUSEHOLD, null) ?: return null
            return DeviceIdentity(household, prefs.getString(KEY_HH_TECH, "").orEmpty())
        }
        set(value) {
            prefs.edit()
                .putString(KEY_HOUSEHOLD, value?.householdId)
                .putString(KEY_HH_TECH, value?.hhTech)
                .apply()
        }

    /** Kanał, dla którego otwarto ostatnią sesję odtwarzania. Trzeba ją zamknąć przed kolejną. */
    var activeChannelId: String?
        get() = prefs.getString(KEY_ACTIVE_CHANNEL, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_CHANNEL, value).apply()

    /** Czyści stan logowania, ale zostawia dane logowania (potrzebne do ponownego zalogowania). */
    fun clearSession() {
        refreshToken = null
        tokenTimeSec = 0L
        identity = null
        activeChannelId = null
    }

    fun clearAll() {
        clearSession()
        credentials = null
    }

    private fun readSecret(key: String): String? = prefs.getString(key, null)?.let(secrets::open)

    private fun writeSecret(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, secrets.seal(value))
        }.apply()
    }

    private companion object {
        const val KEY_LOGIN = "login"
        const val KEY_PIN = "pin"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_TOKEN_TIME = "token_time_sec"
        const val KEY_HOUSEHOLD = "household_id"
        const val KEY_HH_TECH = "hh_tech"
        const val KEY_ACTIVE_CHANNEL = "active_channel"
    }
}
