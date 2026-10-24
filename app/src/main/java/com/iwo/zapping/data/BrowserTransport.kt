package com.iwo.zapping.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class HttpResult(val status: Int, val body: String) {
    /** Odpowiedź zapory przed usługą, nie samej usługi. */
    val isFirewallBlock: Boolean get() = status == 403 && body.contains("supportID")
}

/**
 * Wysyła żądania przez fetch() w ukrytym WebView załadowanym na stronie usługi.
 * Dzięki temu TLS, ciastka (także HttpOnly) i nagłówki są takie jak w prawdziwej
 * przeglądarce i zapora nie odrzuca ruchu. Nie zna żadnych endpointów.
 */
class BrowserTransport(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private val pending = ConcurrentHashMap<String, CompletableDeferred<HttpResult>>()
    private val nextId = AtomicLong()

    private var webView: WebView? = null

    @Volatile
    private var ready = CompletableDeferred<Unit>()

    val userAgent: String = Endpoints.DESKTOP_USER_AGENT

    /** Przy blokadzie zapory przeładowuje stronę (świeże tokeny) i próbuje jeszcze raz. */
    suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): HttpResult {
        val first = execute(method, url, headers, body)
        if (!first.isFirewallBlock) return first
        reload()
        return execute(method, url, headers, body)
    }

    fun cookies(): String = CookieManager.getInstance().getCookie(Endpoints.HOME_URL).orEmpty()

    fun cookie(name: String): String? =
        cookies().split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("$name=") }
            ?.substringAfter("=")
            ?.takeIf { it.isNotEmpty() }

    fun setCookie(raw: String) {
        CookieManager.getInstance().setCookie(Endpoints.HOME_URL, raw)
        CookieManager.getInstance().flush()
    }

    fun expireCookies(vararg names: String) = names.forEach { setCookie("$it=; Max-Age=0; path=/") }

    suspend fun clearCache() {
        withContext(Dispatchers.Main) { webView?.clearCache(true) }
    }

    private suspend fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): HttpResult {
        awaitReady()
        val id = nextId.incrementAndGet().toString()
        val reply = CompletableDeferred<HttpResult>()
        pending[id] = reply
        try {
            val script = fetchScript(id, method, url, headers, body)
            withContext(Dispatchers.Main) { webView?.evaluateJavascript(script, null) }
            val result = withTimeoutOrNull(REQUEST_TIMEOUT_MS) { reply.await() }
                ?: throw RequestFailedException(0, "Usługa nie odpowiedziała na czas.")
            if (result.status < 0) {
                throw RequestFailedException(0, "Błąd połączenia: ${result.body}")
            }
            return result
        } finally {
            pending.remove(id)
        }
    }

    private suspend fun awaitReady() {
        withContext(Dispatchers.Main) { if (webView == null) createWebView() }
        withTimeoutOrNull(PAGE_TIMEOUT_MS) { ready.await() }
            ?: throw RequestFailedException(0, "Nie udało się połączyć z usługą.")
    }

    private suspend fun reload() {
        withContext(Dispatchers.Main) {
            ready = CompletableDeferred()
            val view = webView
            if (view == null) {
                createWebView()
            } else {
                view.clearCache(true)
                view.loadUrl(Endpoints.HOME_URL, NO_CACHE_HEADERS)
            }
        }
        awaitReady()
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun createWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        val view = WebView(context)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        view.settings.userAgentString = userAgent
        view.clearCache(true)
        view.addJavascriptInterface(ResultBridge(), BRIDGE_NAME)
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(page: WebView, url: String) {
                // skrypty strony ustawiają tokeny zapory chwilę po załadowaniu
                main.postDelayed({ if (!ready.isCompleted) ready.complete(Unit) }, SETTLE_DELAY_MS)
            }
        }
        webView = view
        view.loadUrl(Endpoints.HOME_URL, NO_CACHE_HEADERS)
    }

    private fun fetchScript(
        id: String,
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): String {
        val jsId = JSONObject.quote(id)
        val jsBody = if (body == null) "undefined" else JSONObject.quote(body)
        return "(function(){" +
            "fetch(${JSONObject.quote(url)},{method:${JSONObject.quote(method)}," +
            "headers:${JSONObject(headers)},body:$jsBody,credentials:'include',cache:'no-store'})" +
            ".then(function(r){return r.text().then(function(t){$BRIDGE_NAME.onResult($jsId,r.status,t);});})" +
            ".catch(function(e){$BRIDGE_NAME.onResult($jsId,-1,String(e));});" +
            "})();"
    }

    private inner class ResultBridge {
        @JavascriptInterface
        fun onResult(id: String, status: Int, body: String) {
            pending[id]?.complete(HttpResult(status, body))
        }
    }

    private companion object {
        const val BRIDGE_NAME = "TransportBridge"
        const val REQUEST_TIMEOUT_MS = 20_000L
        const val PAGE_TIMEOUT_MS = 20_000L
        const val SETTLE_DELAY_MS = 800L
        val NO_CACHE_HEADERS = mapOf("Cache-Control" to "no-cache", "Pragma" to "no-cache")
    }
}
