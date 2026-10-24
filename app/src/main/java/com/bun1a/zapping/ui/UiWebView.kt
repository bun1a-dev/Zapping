package com.bun1a.zapping.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.bun1a.zapping.data.Endpoints

/** Konfiguracja WebView z interfejsem. Pliki z assets/ui są serwowane lokalnie, bez sieci. */
object UiWebView {

    private const val ENTRY_URL = "https://appassets.androidplatform.net/assets/ui/index.html"

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(webView: WebView) {
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(webView.context))
            .build()

        webView.setBackgroundColor(Color.TRANSPARENT)   // przez to widać wideo pod interfejsem
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true          // szanuj width=1920 z meta viewport
        webView.settings.loadWithOverviewMode = true     // dopasuj powiększenie do ekranu
        webView.settings.textZoom = 100                  // ignoruj systemowe skalowanie czcionek
        webView.settings.userAgentString = Endpoints.DESKTOP_USER_AGENT   // logotypy też idą przez zaporę
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)
        }
    }

    fun load(webView: WebView) = webView.loadUrl(ENTRY_URL)
}
