package com.bun1a.zapping.data

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
    /** Kanał ma wersje regionalne (np. TVP 3). */
    val isRegional: Boolean,
)

/** Wersja regionalna kanału. [suffix] dopisuje się do identyfikatora kanału w adresie strumienia. */
data class Region(val name: String, val suffix: String)

data class Program(val id: String, val title: String, val startSec: Long, val endSec: Long)

/** Na żywo, gdy [programId] jest null. Inaczej odtwarzanie z archiwum. */
data class StreamRequest(
    val channelId: String,
    val programId: String? = null,
    val startSec: Long? = null,
    val endSec: Long? = null,
    /** Nazwa wersji regionalnej (tylko na żywo, tylko kanały regionalne). */
    val region: String? = null,
) {
    val isCatchup: Boolean get() = programId != null
}

data class StreamTicket(val manifestUrl: String, val licenseUrl: String)
