<h1>
  Zapping
  <img src="https://github.com/bun1a-dev/Zapping/blob/main/branding/banner.png?raw=true"
       width="200"
       align="right"
       alt="Zapping banner">
</h1>

Nieoficjalny klient Orange TV GO na Google TV i Android TV. Kanały na żywo, archiwum programów i przewodnik, w całości do obsługi pilotem.

> Zapping jest niezależnym projektem. Nie jest powiązany z Orange Polska ani przez nią wspierany. Orange i Orange TV GO są znakami towarowymi ich właściciela i są tu użyte wyłącznie do opisania zgodności. Do korzystania z aplikacji potrzebna jest własna, aktywna subskrypcja.

## Funkcje

- Kanały na żywo z panelem "teraz leci", godzinami programu i paskiem postępu.
- Archiwum: wybór kanału, dnia i zakończonego programu.
- Szybka zmiana kanału strzałkami, przyciskami CH+ i CH- oraz cyframi z pilota.
- Interfejs zaprojektowany pod telewizor: duże elementy, wyraźny fokus, pełna obsługa bez myszy i ekranu dotykowego.
- Dane logowania szyfrowane kluczem z Android Keystore. Aplikacja nie wysyła ich nigdzie poza usługę, z którą się łączy.

## Wymagania

- Android 8.0 (API 26) lub nowszy.
- Urządzenie z obsługą Widevine.
- Aktywna subskrypcja Orange TV GO.
- Android Studio (do zbudowania ze źródeł).

## Instalacja
Gotowa wersja aplikacji jest dostępna w sekcji Releases.

1. Otwórz najnowsze wydanie.
2. W sekcji Assets pobierz plik app-debug.apk.
3. Zainstaluj plik na urządzeniu z Google TV lub Android TV.

Nie musisz instalować Android Studio ani samodzielnie kompilować aplikacji, aby z niej korzystać.

Z Android Studio:

1. Otwórz katalog projektu i poczekaj na synchronizację Gradle.
2. Wybierz urządzenie (telewizor, emulator Android TV lub telefon) i uruchom aplikację.

Z linii poleceń, na telewizorze z włączonym debugowaniem przez sieć:

```
./gradlew assembleDebug
adb connect ADRES_TELEWIZORA:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Przy pierwszym uruchomieniu zaloguj się loginem i PIN-em z Orange TV GO.

## Sterowanie

| Klawisz pilota | Działanie |
| --- | --- |
| Strzałki | Poruszanie się po ekranie |
| OK | Wybór, odtwarzanie |
| Wstecz | Cofnięcie o ekran, na ekranie głównym zamknięcie aplikacji |
| Cyfry 0 do 9 | Wpisanie numeru kanału. Lista ustawia fokus na kanale, odtwarzacz przełącza |
| Góra i dół w odtwarzaczu | Następny i poprzedni kanał |
| CH+ i CH- | Następny i poprzedni kanał (na pilotach, które je mają) |

## Architektura

Interfejs to zwykłe pliki HTML, CSS i JavaScript wyświetlane w WebView, bez frameworków i narzędzi do budowania. Kotlin odpowiada za dane, sesję i odtwarzanie. Obraz gra w natywnym ExoPlayerze pod przezroczystym interfejsem.

```
app/src/main
├── assets/ui                    interfejs
│   ├── js/screens               jeden plik na ekran
│   ├── js/native.js             most do Kotlina (js/mock.js: atrapa do podglądu)
│   └── css                      tokeny, baza i style ekranów
└── java/com/iwo/zapping
    ├── data                     transport, endpointy, sesja, szyfrowany magazyn
    ├── bridge                   most JS i Kotlin oraz lista metod (Routes.kt)
    ├── player                   ExoPlayer, DRM, koordynator odtwarzania
    └── ui                       konfiguracja WebView
```

Założenia, których warto się trzymać:

- Adresy usługi są wyłącznie w `data/Endpoints.kt`.
- Logowanie, odświeżanie tokenu i ponowne logowanie są wyłącznie w `data/Session.kt`.
- Żądania do usługi idą przez `fetch` w ukrytym WebView (`data/BrowserTransport.kt`). Dzięki temu ruch jest nie do odróżnienia od zwykłej przeglądarki.
- Interfejs i Kotlin rozmawiają przez trzy rzeczy: wywołanie metody, odpowiedź i zdarzenie. Cały zestaw metod to jeden plik.

## Podgląd interfejsu w przeglądarce

Interfejs działa także bez Androida, na danych przykładowych:

```
cd app/src/main/assets/ui
python -m http.server 8000
```

Otwórz `http://localhost:8000`, przejdź do trybu pełnoekranowego i steruj strzałkami, klawiszem Enter i Escape.

## Znane ograniczenia

- Maksymalna jakość obrazu to 720p. Usługa przydziela jakość według typu klienta, a aplikacja zgłasza się jako przeglądarka.
- Login i PIN wpisuje się klawiaturą numeryczną, więc pole przyjmuje tylko cyfry.
- Regionalne wersje TVP3 nie są jeszcze obsługiwane.
- CH+ i CH- działają tylko na pilotach, które mają te przyciski. Pilot Chromecasta z Google TV ich nie ma.

## Zastrzeżenie

Aplikacja komunikuje się z usługą w sposób, który nie jest publicznie udokumentowany. Może przestać działać po zmianach po stronie operatora. Korzystasz z niej na własną odpowiedzialność i zgodnie z regulaminem swojej subskrypcji.
