package com.iwo.zapping.data

data class Credentials(val login: String, val pin: String)

/** Dane gospodarstwa potrzebne do nagłówka identyfikującego urządzenie. */
data class DeviceIdentity(val householdId: String, val hhTech: String)

data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val category: String,
    val logoUrl: String,
    /** Ile dni wstecz działa archiwum. 0 oznacza brak archiwum. */
    val catchupDays: Int,
)

data class Program(val id: String, val title: String, val startSec: Long, val endSec: Long)

/** Na żywo, gdy [programId] jest null. Inaczej odtwarzanie z archiwum. */
data class StreamRequest(
    val channelId: String,
    val programId: String? = null,
    val startSec: Long? = null,
    val endSec: Long? = null,
) {
    val isCatchup: Boolean get() = programId != null
}

data class StreamTicket(val manifestUrl: String, val licenseUrl: String)
