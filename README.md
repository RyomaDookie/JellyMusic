# JellyMusic - Android Jellyfin Music Player

Un lettore musicale moderno, open source per Android realizzato con **Jetpack Compose**, **AndroidX Media3 (ExoPlayer)** e connettività diretta a server **Jellyfin**.

---

## Caratteristiche & Customizzazioni

### 🎨 Personalizzazione Visiva & Temi
* **Modalità Tema**:
  * `AMOLED`: Nero assoluto (#000000) ottimizzato per schermi OLED e risparmio batteria.
  * `Dark`: Tema scuro moderno Material 3.
  * `Light`: Tema chiaro.
  * `System`: Segue la modalità del sistema operativo.
* **Tavolozze di Colori (Accent Palettes)**:
  * `Dynamic / Material You`: Estrae i colori dinamici dallo sfondo di sistema (Android 12+).
  * `Jellyfin Purple`: Accento viola/blu caratteristico Jellyfin.
  * `Cyberpunk Neon Cyan`: Contrasto ciano neon e magenta.
  * `Sunset Amber`: Arancio tramonto e oro caldo.
  * `Emerald Forest`: Menta e verde smeraldo profondo.
  * `Sakura Pink`: Toni rosa ciliegio.
* **Stili di Layout del Player**:
  * `Modern Hero`: Grande copertina centrale con ombre elevate ed effetto immersivo.
  * `Compact Card`: Copertina a scheda compatta con elevazione arrotondata.
  * `Minimalist`: Design minimale focalizzato sui dettagli tipografici e controlli essenziali.
* **Raggio di Arrotondamento (Corner Radius)**:
  * Slider personalizzabile da 4dp a 28dp per personalizzare l'aspetto delle schede e dei pulsanti.

---

### 🎵 Funzionalità Audio & Riproduzione
* **AndroidX Media3 (ExoPlayer & MediaSessionService)**:
  * Riproduzione audio continua in background.
  * Notifica multimediale di sistema con controlli di riproduzione e copertina.
  * Controlli completi nella schermata di blocco e gestione "becoming noisy" (pausa automatica se si scollegano le cuffie).
* **Qualità di Streaming personalizzabile**:
  * `Direct Stream`: Flusso audio originale / lossless (FLAC, ALAC, MP3 originale).
  * `High (320 kbps)`: Transcoding MP3 ad alta definizione su connessioni mobili.
  * `Medium (192 kbps)`: Bilanciato per streaming rapido.
  * `Data Saver (128 kbps)`: Risparmio dati.
* **Velocità di riproduzione regolabile**: 0.75x, 1.0x, 1.25x, 1.5x.
* **Scrubber interattivo, Riproduzione Casuale (Shuffle) e Ripeti (Repeat One / All)**.

---

### 🌐 Connessione a Server Jellyfin
* Supporto per connessioni sicure HTTPS o HTTP (locale/remoto).
* Autenticazione tramite endpoint nativo `/Users/AuthenticateByName`.
* Sfoglia per Brani musicali o Album con caricamento copertine e ricerca in tempo reale.
* Disconnessione e gestione account dalle impostazioni.

---

## Requisiti e Compilazione

* **Android Studio**: Ladybug / Meerkat o successivo (o CLI Gradle con JDK 17+).
* **Target SDK**: Android 15 (API 35).
* **Min SDK**: Android 8.0 Oreo (API 26).

### Come aprire ed eseguire il progetto:
1. Apri **Android Studio**.
2. Seleziona **File > Open** e scegli la cartella `jellyfin-player`.
3. Attendi la sincronizzazione Gradle.
4. Collega il tuo dispositivo Android o avvia un emulatore e premi **Run (Shift + F10)**.
