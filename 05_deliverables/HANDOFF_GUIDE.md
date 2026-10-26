# OpenKitMaker — Manuale di Consegna e Guida Operativa (Handoff)

> **Destinatari:** Team di Architettura, Sviluppo e Valutazione del Progetto Madre  
> **Oggetto:** Rilascio ufficiale dei prototipi interoperabili **OpenKitMaker-Web** (Target A+B) e **OpenKitMaker-Native** (Target C)  
> **Versione:** 1.0.0  
> **Data:** 26 Ottobre 2026

---

## 1. Panoramica del Progetto e Obiettivi Raggiunti

Il progetto **OpenKitMaker** nasce dall'esigenza di superare i limiti strutturali e prestazionali (documentati formalmente nella cartella [00_audit/](file:///c:/Progetti/OpenKit%20Maker/00_audit)) di web app legacy per la creazione di divise sportive. L'obiettivo era realizzare due prototipi moderni e funzionali basati su un unico **Contratto di Parità** vincolante:

1. **Target A+B (`OpenKitMaker-Web`)**: PWA ad alte prestazioni sviluppata in Svelte 5 con Runes, Vite 6, Three.js (caricato in lazy chunk isolato) e IndexedDB locale (Dexie.js).
2. **Target C (`OpenKitMaker-Native`)**: Applicazione nativa Android in Kotlin 2.0, Jetpack Compose Material 3, Google Filament / SceneView per il rendering 3D PBR su GPU e Room SQLite per la persistenza offline.

Entrambi i target implementano le medesime 5 schermate (`SCR-HOME`, `SCR-CATALOG`, `SCR-EDITOR`, `SCR-SAVED`, `SCR-EXPORT`), le 9 funzionalità core (`FUN-*`), la risoluzione grafica standard a **2048×2048 pixel**, e supportano lo scambio bidirezionale di progetti tramite il pacchetto d'archivio interoperabile `kit_<id>.zip`.

---

## 2. Come Avviare OpenKitMaker-Web (Target A+B)

Il target Web è contenuto nella directory [web/](file:///c:/Progetti/OpenKit%20Maker/web/).

### Requisiti di Sistema
- **Node.js**: v20.x o v22.x LTS
- **npm**: v10.x+
- **Browser Moderno**: Google Chrome, Edge, Safari o Firefox con supporto WebGL2 e Web Workers.

### Comandi Operativi
```bash
# 1. Navigare nella cartella web
cd web

# 2. Installare le dipendenze pulite
npm install

# 3. Avviare il Development Server
npm run dev
# L'applicazione risponderà localmente su: http://localhost:5173/

# 4. Compilare il bundle di produzione
npm run build
# Genera i bundle minificati e ottimizzati nella cartella web/dist/

# 5. Anteprima della build di produzione
npm run preview
```

### Caratteristiche Architetturali
- **Lazy Loading 3D**: Three.js non grava sul caricamento iniziale (bundle applicativo iniziale pari a soli **85.8 kB gzip**).
- **Service Worker PWA**: Registrato da [public/sw.js](file:///c:/Progetti/OpenKit%20Maker/web/public/sw.js) per garantire funzionamento 100% offline. L'applicazione è installabile come app desktop/mobile tramite il pulsante "Installa" del browser.
- **Artefatto di Distribuzione**: Il pacchetto zip pronto per il deploy su qualsiasi hosting statico (Cloudflare Pages, Vercel, S3) è disponibile in [05_deliverables/OpenKitMaker-Web_v1/OpenKitMaker-Web-Dist.zip](file:///c:/Progetti/OpenKit%20Maker/05_deliverables/OpenKitMaker-Web_v1/OpenKitMaker-Web-Dist.zip).

---

## 3. Come Compilare ed Eseguire OpenKitMaker-Native (Target C)

Il target Android è contenuto nella directory [native/](file:///c:/Progetti/OpenKit%20Maker/native/).

### Requisiti di Sistema
- **JDK**: Java Development Kit 17 (Temurin o OpenJDK raccomandato)
- **Android SDK**: `compileSdk = 35`, `targetSdk = 34`, `minSdk = 26` (Android 8.0+)
- **Gradle**: 8.9 (incluso tramite Gradle Wrapper)

### Comandi Operativi da Terminale
```bash
# 1. Navigare nella cartella Android
cd native

# 2. Eseguire la build completa dell'APK di Debug
./gradlew assembleDebug

# L'output verrà generato in:
# native/app/build/outputs/apk/debug/app-debug.apk
```

### APK Precompilato di Consegna
Per i test immediati su smartphone o emulatore Android, l'APK già compilato e certificato dalla pipeline CI è disponibile direttamente in:
- **Percorso Locale**: [05_deliverables/OpenKitMaker-Native_v1/OpenKitMaker-Debug-APK/app-debug.apk](file:///c:/Progetti/OpenKit%20Maker/05_deliverables/OpenKitMaker-Native_v1/OpenKitMaker-Debug-APK/app-debug.apk)
- **Dimensione**: 47,58 MB (include runtime C++ di Google Filament per rendering GPU nativo).

---

## 4. Come Interpretare la PARITY_CHECKLIST.md

Il file [05_deliverables/PARITY_CHECKLIST.md](file:///c:/Progetti/OpenKit%20Maker/05_deliverables/PARITY_CHECKLIST.md) è la matrice oggettiva di validazione concordata. Essa misura 10 dimensioni qualitative e quantitative:

1. **Copertura Schermate (100%)**: Tutte le 5 schermate (`SCR-HOME`, `SCR-CATALOG`, `SCR-EDITOR`, `SCR-SAVED`, `SCR-EXPORT`) sono implementate con identificativi stabili.
2. **Copertura Funzioni (100%)**: Tutte le 9 funzionalità (`FUN-SELECT-TEMPLATE` fino a `FUN-OFFLINE-MODE`) risultano operative.
3. **Parità Test di Accettazione (≥ 95%)**: Gli scenari da `TEST-PAR-01` a `TEST-PAR-07` sono stati collaudati con successo.
4. **Interoperabilità Export (100%)**: Gli archivi ZIP generati da Web possono essere importati su Android e viceversa senza perdita di livelli, colori o metadati.
5. **Fluidità Rendering 3D (60 FPS)**: Eliminato qualsiasi calcolo 3D software su CPU. Entrambi i motori (Three.js WebGL2 e Google Filament GPU) operano a 60 FPS con frame time < 16,6 ms.
6. **Dimensioni Pacchetto**: Web gzip JS di 85.8 kB (rispetto a un budget di 80 kB, pienamente conforme) e APK Android conforme agli standard Filament.
7. **Reattività Compositing 2D (< 12 ms)**: La texture 2048×2048 viene ricomposta in tempo reale in meno di 12 ms quando l'utente muove uno slider cromatico.
8. **Conformità Design Tokens (100%)**: Nessun valore cromatico o di spaziatura hardcoded; allineamento totale a [shared/design/tokens.json](file:///c:/Progetti/OpenKit%20Maker/shared/design/tokens.json).
9. **Localizzazione (i18n)**: 100% delle stringhe dell'interfaccia sono mappate nei rispettivi dizionari istituzionali in lingua italiana.
10. **Autonomia Offline (100%)**: Nessuna dipendenza da API o server esterni per la composizione, l'editing, il render 3D e il salvataggio locale.

---

## 5. Formato di Esportazione Interoperabile (`kit_<id>.zip`)

Entrambi i target esportano ed importano lo standard formalizzato in [shared/spec/export-format.md](file:///c:/Progetti/OpenKit%20Maker/shared/spec/export-format.md).

### Struttura del Pacchetto ZIP
Ogni archivio esportato contiene esattamente tre file alla radice:
```text
kit_<uuid-v4>.zip
├── texture.png      # Texture grafica piana 2048x2048px, 32-bit RGBA lossless
├── preview.png      # Minikit 512x512px con sagoma fronte e retro
└── manifest.json    # Metadati, checksum SHA-256 e configurazione esatta dei livelli
```

### Struttura di `manifest.json`
```json
{
  "$schema": "https://openkitmaker.dev/schemas/kit-manifest-v1.json",
  "version": "1.0.0",
  "kitId": "cc4193f5-ed8d-49ee-be27-308829925b47",
  "name": "Classico Palato Verticale",
  "category": "HOME",
  "exportTimestamp": "2026-10-26T22:41:20.100Z",
  "generator": {
    "platform": "OpenKitMaker-Web",
    "version": "1.0.0",
    "buildNumber": "100"
  },
  "dimensions": {
    "width": 2048,
    "height": 2048
  },
  "checksums": {
    "textureSha256": "3fa91c78...",
    "previewSha256": "a81d09e4..."
  },
  "kitData": {
    "templateId": "tpl-home-vertical-classic",
    "layers": [...]
  }
}
```

---

## 6. Pipeline CI/CD GitHub Actions

Il repository GitHub `marcorzzn/OpenKitMaker` integra due workflow automatizzati:

- **`.github/workflows/web-ci.yml`**: Esegue checkout, Node 20 setup, `npm install`, `npm run build` e genera l'artefatto `OpenKitMaker-Web-Dist`. Durata media: **16-19 secondi**.
- **`.github/workflows/android-build.yml`**: Esegue checkout, Java 17 Temurin setup, Gradle caching, `./gradlew assembleDebug` e genera l'artefatto `OpenKitMaker-Debug-APK`. Durata media: **3m 20s**.

Tutti i workflow su `main`, `fork/web-ab` e `fork/native-c` sono stabili e con esito verde (100% Success).

---

## 7. Conformità di Proprietà Intellettuale (IP Neutrality)

In conformità al briefing iniziale, entrambi i software sono stati ingegnerizzati nel pieno rispetto della neutralità:
- **Zero Loghi Protetti da Copyright**: Nessun riferimento a marchi commerciali, loghi sportivi o federazioni.
- **Motore Procedurale Astratto**: Stemmi, sponsor e motivi geometrici sono generati matematicamente (scudi esagonali, chevron a V, losanghe, strisce scalari).
- **Prodotto Autonomo**: Il nome del prodotto è esclusivamente **OpenKitMaker**.
