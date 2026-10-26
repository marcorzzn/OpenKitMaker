# OpenKitMaker — Ramo Web (Target A+B: Svelte 5 + PWA/TWA)

Questo ramo contiene l'implementazione Web ad alte prestazioni del contratto unificato **OpenKitMaker**.

## Architettura e Stack
- **Framework**: Svelte 5 con Runes (`$state`, `$derived`, `$effect`) su Vite minimale (SPA pura, no SSR).
- **Grafica 3D**: Three.js (WebGL2) caricato in lazy loading con KTX2Loader e DRACOLoader.
- **Storage Locale**: IndexedDB tramite Dexie.
- **PWA & TWA**: Service worker offline-first e packaging Android tramite Google Bubblewrap.

## Comandi Rapidi
```bash
npm install
npm run dev        # Avvia dev server locale
npm run build      # Build di produzione (target JS iniziale <= 80 KB gzip)
npm run preview    # Test della PWA con simulazione offline
npm test           # Test unitari con Vitest
npm run test:e2e   # Test di parità funzionale con Playwright
```

## Riferimenti di Parità
Tutti i componenti implementano gli ID stabili definiti in `../shared/spec/screens.md` e `../shared/spec/features.md`.
