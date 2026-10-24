package com.bun1a.zapping

import android.content.Context
import com.bun1a.zapping.data.BrowserTransport
import com.bun1a.zapping.data.ServiceApi
import com.bun1a.zapping.data.Session
import com.bun1a.zapping.data.SessionStore

/** Obiekty żyjące tak długo jak proces. Ręczne wstrzykiwanie zależności wystarcza przy tej wielkości. */
class AppGraph(context: Context) {
    val transport = BrowserTransport(context)
    val store = SessionStore(context)
    val api = ServiceApi(transport) { store.identity }
    val session = Session(store, api, transport)
}
