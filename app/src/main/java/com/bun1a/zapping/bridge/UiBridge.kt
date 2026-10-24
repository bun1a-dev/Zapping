package com.bun1a.zapping.bridge

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.bun1a.zapping.data.ServiceException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Most między Kotlinem a interfejsem w WebView. Trzy rzeczy i nic więcej:
 *  - interfejs wywołuje metodę: żądanie {id, method, params}, odpowiedź {id, ok, result|error},
 *  - Kotlin wysyła zdarzenie: {event, payload},
 *  - metody rejestruje się w [register]. Zestaw metod jest w Routes.kt.
 */
class UiBridge @SuppressLint("JavascriptInterface") constructor(private val webView: WebView) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handlers = HashMap<String, suspend (JSONObject) -> Any?>()

    init {
        webView.addJavascriptInterface(this, JS_NAME)
    }

    fun register(method: String, handler: suspend (JSONObject) -> Any?) {
        handlers[method] = handler
    }

    fun emit(event: String, payload: JSONObject = JSONObject()) {
        deliver(JSONObject().put("event", event).put("payload", payload))
    }

    fun close() = scope.cancel()

    /** Wołane z wątku mostu JS, dlatego od razu przenosimy pracę na wątek główny. */
    @JavascriptInterface
    fun post(message: String) {
        val request = JSONObject(message)
        val id = request.getString("id")
        val method = request.getString("method")
        val params = request.optJSONObject("params") ?: JSONObject()

        scope.launch {
            val reply = JSONObject().put("id", id)
            try {
                val handler = handlers[method] ?: throw IllegalArgumentException("Nieznana metoda: $method")
                val result = handler(params)
                reply.put("ok", true).put("result", if (result == null || result is Unit) JSONObject.NULL else result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ServiceException) {
                reply.put("ok", false).put("error", errorJson(e.code, e.message))
            } catch (e: Exception) {
                Log.e(TAG, "Metoda $method zakończyła się błędem", e)
                reply.put("ok", false).put("error", errorJson("unexpected", e.message))
            }
            deliver(reply)
        }
    }

    private fun errorJson(code: String, message: String?) = JSONObject()
        .put("code", code)
        .put("message", message ?: "Nieznany błąd.")

    private fun deliver(message: JSONObject) {
        val script = "window.zapping.receive(${JSONObject.quote(message.toString())})"
        webView.post { webView.evaluateJavascript(script, null) }
    }

    private companion object {
        const val JS_NAME = "NativeBridge"
        const val TAG = "Zapping"
    }
}
