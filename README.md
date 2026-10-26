# OpenKitMaker

> **Studio Grafico 2D/3D Professionale & PWA Offline-First per Divise Sportive**  
> **Creatore & Lead Architect:** [Marco Razzano](https://github.com/marcorzzn) (`@marcorzzn`)  
> **Licenza:** [MIT](LICENSE)

---

## Panoramica

**OpenKitMaker** è una piattaforma indipendente di nuova generazione per la progettazione sartoriale, personalizzazione grafica e visualizzazione 3D ad alte prestazioni di divise e kit sportivi. 

Progettato con un'architettura **Offline-First**, OpenKitMaker garantisce autonomia operativa totale senza dipendere da backend esterni, compositando texture grafiche ultra-dettagliate a **2048×2048 pixel RGBA** e renderizzandole in tempo reale a **60 FPS** su modelli tridimensionali realistici con illuminazione fisicamente accurata (PBR).

---

## Architettura Multi-Piattaforma

Il progetto implementa un unico **Design System e Specifiche Ufficiali** condivise (cartella [`shared/`](shared/)), declinato su due target ad alte prestazioni:

```text
OpenKitMaker/
│
├── web/               # Target A+B: Svelte 5 + Vite 6 + Three.js PBR + Dexie IndexedDB + PWA
├── native/            # Target C: Android Nativo (Kotlin 2.0 + Jetpack Compose + Google Filament + Room)
├── shared/            # Specifiche e Design System Ufficiale OpenKitMaker (tokens, spec, i18n)
└── 05_deliverables/   # Guide di rilascio, checklist di qualità e pacchetti pronti
```

### 1. OpenKitMaker-Web (Target A+B)
- **Framework**: Svelte 5 con Runes (`$state`, `$derived`, `$effect`) su Vite 6.
- **Rendering 3D**: Three.js WebGL2 in chunk asincrono lazy (`three-engine`), con shader PBR, preset illuminazione *Studio* (6500K) e *Stadio* (5400K) e normal map procedurale per tessuto traspirante.
- **Motore 2D Compositor**: Canvas 2048×2048 px ad alta precisione con testo dorsale curvato su arco (`arcText`) e numeri bicolore con contorno vettoriale (`outline`).
- **Storage Locale**: IndexedDB tramite Dexie.js.
- **PWA Ready**: Service Worker Workbox con precaching dell'App Shell e funzionamento 100% offline. Installabile standalone su Desktop e Mobile.

### 2. OpenKitMaker-Native (Target C — Android)
- **Framework**: Kotlin 2.0.21, Jetpack Compose Material 3, Hilt DI, Coroutines & Flow.
- **Rendering 3D Hardware GPU**: **Google Filament / SceneView 2.2.1** su Vulkan / OpenGL ES 3.0. Nessun utilizzo di rendering software CPU (60 FPS stabili con matrici pre-allocate).
- **Motore 2D Hardware**: `TextureEngine.kt` con accelerazione hardware su Android `Bitmap` / `Canvas`.
- **Persistenza Locale**: SQLite Room con salvataggio automatico e thumbnail minikit 512×512.
- **Export Asincrono**: WorkManager con Scoped Storage Android 10+ (`MediaStore.Downloads`).

---

## Standard di Esportazione Interoperabile (`kit_<id>.zip`)

Entrambi i software esportano ed importano il medesimo archivio standard universale:
```text
kit_<uuid>.zip
├── texture.png      # Texture grafica piana 2048x2048px, 32-bit RGBA lossless
├── preview.png      # Minikit fronte/retro 512x512px
└── manifest.json    # Schema kit-manifest-v1.json + hash crittografici SHA-256
```
Qualsiasi kit salvato su Web può essere aperto e modificato su Android e viceversa, garantendo la totale assenza di vendor lock-in.

---

## Quickstart

### Avvio Web (Svelte 5)
```bash
cd web
npm install
npm run dev
# Accedi da browser a: http://localhost:5173/
```

### Compilazione Android (Gradle)
```bash
cd native
./gradlew assembleDebug
# APK pronto in native/app/build/outputs/apk/debug/app-debug.apk
```

---

## Risorse e Documentazione

- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) — Guida alla pubblicazione su Vercel, Netlify e Google Play Store.
- [ROADMAP.md](ROADMAP.md) — Visione di prodotto, monetizzazione e roadmap commerciale.
- [05_deliverables/PARITY_CHECKLIST.md](05_deliverables/PARITY_CHECKLIST.md) — Matrice di collaudo e qualità del Design System.
- [05_deliverables/HANDOFF_GUIDE.md](05_deliverables/HANDOFF_GUIDE.md) — Manuale operativo di consegna architetturale.
- [LICENSE](LICENSE) — Licenza MIT.

---

## Autore

**Marco Razzano**  
GitHub: [@marcorzzn](https://github.com/marcorzzn)  
Progetto: [OpenKitMaker](https://github.com/marcorzzn/OpenKitMaker)
