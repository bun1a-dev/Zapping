package com.iwo.zapping

import android.content.Context
import com.iwo.zapping.data.BrowserTransport
import com.iwo.zapping.data.ServiceApi
import com.iwo.zapping.data.Session
import com.iwo.zapping.data.SessionStore

/** Obiekty żyjące tak długo jak proces. Ręczne wstrzykiwanie zależności wystarcza przy tej wielkości. */
class AppGraph(context: Context) {
    val transport = BrowserTransport(context)
    val store = SessionStore(context)
    val api = ServiceApi(transport) { store.identity }
    val session = Session(store, api, transport)
}
