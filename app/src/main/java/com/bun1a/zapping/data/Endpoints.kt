package com.bun1a.zapping.data

/** Jedyne miejsce z adresami i stałymi usługi. Reszta kodu nie zna żadnych URL-i. */
internal object Endpoints {
    const val ORIGIN = "https://tvgo.orange.pl"
    const val HOME_URL = "$ORIGIN/"
    const val API = "$ORIGIN/gpapi2/"
    const val IMAGES = "$ORIGIN/gpapi/resource/image/"
    const val LICENSE = "$ORIGIN/RTEFacade_RIGHTV/widevinedrm"

    const val DEVICE_TYPE = "web_otf"
    const val DEVICE_CATEGORY = "otg"
    const val APP_VERSION = "1.21.1"
    const val TERMINAL_SERIAL_PREFIX = "FIREFOX_WEB_TERMINAL_1"

    /**
     * Usługa odrzuca mobilne i wbudowane przeglądarki, więc wszędzie podajemy się
     * za zwykłą przeglądarkę desktopową.
     */
    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
}
