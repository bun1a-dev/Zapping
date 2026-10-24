package com.bun1a.zapping

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.media3.ui.PlayerView
import com.bun1a.zapping.bridge.UiBridge
import com.bun1a.zapping.bridge.registerRoutes
import com.bun1a.zapping.player.PlaybackCoordinator
import com.bun1a.zapping.player.PlayerController
import com.bun1a.zapping.ui.UiWebView
import org.json.JSONObject

/**
 * Jedyny ekran aplikacji. Składa warstwy: odtwarzacz na dole, interfejs w WebView na górze.
 * Cała logika jest w klasach, które tu tylko łączymy.
 */
class MainActivity : ComponentActivity() {

    private lateinit var uiWebView: WebView
    private lateinit var bridge: UiBridge
    private lateinit var player: PlayerController
    private lateinit var playback: PlaybackCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val graph = (application as ZappingApp).graph
        uiWebView = findViewById(R.id.ui_web_view)
        UiWebView.configure(uiWebView)

        bridge = UiBridge(uiWebView)
        player = PlayerController(
            context = this,
            emit = { event, payload -> bridge.emit(event, payload) },
            keepScreenOn = { on -> applyKeepScreenOn(on) },
        )
        player.attach(findViewById<PlayerView>(R.id.player_view))
        playback = PlaybackCoordinator(graph.session, graph.api, graph.store, graph.transport, player)
        registerRoutes(bridge, graph, playback)

        onBackPressedDispatcher.addCallback(this) { askUiToGoBack() }
        UiWebView.load(uiWebView)
    }

    override fun onResume() {
        super.onResume()
        uiWebView.requestFocus()
    }

    /** Po wyjściu z aplikacji nie trzymamy otwartego strumienia. */
    override fun onStop() {
        super.onStop()
        playback.stop()
        bridge.emit("player.stopped")
    }

    override fun onDestroy() {
        playback.close()
        player.release()
        bridge.close()
        super.onDestroy()
    }

    /** Przyciski CH+ i CH- nie docierają do strony jako klawisze, więc przekazujemy je sami. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val key = when (event.keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> "ChannelUp"
                KeyEvent.KEYCODE_CHANNEL_DOWN -> "ChannelDown"
                else -> null
            }
            if (key != null) {
                bridge.emit("key", JSONObject().put("key", key))
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /** Telewizor nie włącza wygaszacza ekranu podczas oglądania, a w menu działa normalnie. */
    private fun applyKeepScreenOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** Interfejs decyduje, czy Wstecz cofa go o ekran. Jeśli nie ma dokąd, zamykamy aplikację. */
    private fun askUiToGoBack() {
        uiWebView.evaluateJavascript("window.zapping.onBack()") { handled ->
            if (handled != "true") finish()
        }
    }
}
