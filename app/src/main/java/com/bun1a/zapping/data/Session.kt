package com.bun1a.zapping.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Jedyne miejsce, które loguje, odświeża token i ponawia logowanie.
 * Reszta aplikacji woła tylko [authorized] albo [signIn] i [signOut].
 */
class Session(
    private val store: SessionStore,
    private val api: ServiceApi,
    private val transport: BrowserTransport,
) {
    private val lock = Mutex()

    val hasCredentials: Boolean get() = store.credentials != null

    /** Zapisuje dane logowania dopiero po udanym zalogowaniu. */
    suspend fun signIn(credentials: Credentials): Unit = lock.withLock {
        loginWith(credentials)
        store.credentials = credentials
    }

    suspend fun signOut(): Unit = lock.withLock {
        runCatching { api.logout() }
        transport.expireCookies("Token", "TokenId", "Authenticated")
        store.clearAll()
    }

    /** Pilnuje ważności sesji przed wywołaniem [block]. Jedno ponowne logowanie przy 401. */
    suspend fun <T> authorized(block: suspend () -> T): T {
        ensureActive()
        return try {
            block()
        } catch (e: UnauthorizedException) {
            lock.withLock { loginWith(store.credentials ?: throw NotLoggedInException()) }
            block()
        }
    }

    private suspend fun ensureActive(): Unit = lock.withLock {
        val credentials = store.credentials ?: throw NotLoggedInException()
        val loggedIn = store.identity != null && transport.cookie("Token") != null
        if (!loggedIn) {
            loginWith(credentials)
            return@withLock
        }

        val ageSec = nowSec() - store.tokenTimeSec
        when {
            ageSec >= REFRESH_EXPIRES_SEC -> loginWith(credentials)
            ageSec >= REFRESH_AFTER_SEC -> refreshOrLogin(credentials)
            else -> Unit
        }
    }

    private suspend fun refreshOrLogin(credentials: Credentials) {
        val token = store.refreshToken?.let { api.refresh(it) }
        if (token == null) {
            loginWith(credentials)
        } else {
            store.refreshToken = token
            store.tokenTimeSec = nowSec()
        }
    }

    private suspend fun loginWith(credentials: Credentials) {
        store.clearSession()
        val refreshToken = api.login(credentials)
        transport.setCookie("Authenticated=true; path=/")
        store.identity = api.household()
        store.refreshToken = refreshToken
        store.tokenTimeSec = nowSec()
    }

    private fun nowSec() = System.currentTimeMillis() / 1000

    private companion object {
        const val REFRESH_AFTER_SEC = 24L * 60 * 60
        const val REFRESH_EXPIRES_SEC = 14L * 24 * 60 * 60
    }
}
