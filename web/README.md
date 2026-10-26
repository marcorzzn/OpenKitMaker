# OpenKitMaker — Ramo Web (Target A+B: Svelte 5 + PWA/TWA)

Questo ramo contiene l'implementazione Web ad alte prestazioni del contratto unificato **OpenKitMaker**.

## Architettura e Stack Tecnologico
- **Framework**: Svelte 5 con Runes (`$state`, `$derived`, `$effect`) su Vite 6 (SPA pura, no SSR).
- **Grafica 3D**: Three.js (WebGL2) in chunk separato (`three-engine`) con PBR materials, KTX2Loader, DRACOLoader e illuminazione Studio/Stadium.
- **Motore 2D Compositor**: Canvas 2048x2048px ad alta precisione con supporto a pattern geometrici, colletti, polsini, loghi geometrici procedurali e testo curvato.
- **Storage Locale**: IndexedDB tramite Dexie per persistenza offline dei kit.
- **Esportazione Interoperabile**: Supporto nativo export PNG 2048x2048 e pacchetti `kit_<id>.zip` conformi alle specifiche di parità.
- **PWA**: Service Worker (`public/sw.js`) per operatività 100% offline.

## Comandi Rapidi
```bash
npm install
npm run dev        # Avvia dev server locale su http://localhost:5173
npm run build      # Build di produzione
npm run preview    # Anteprima della build di produzione
npm run lint       # Typecheck con TypeScript
```

## Riferimenti di Parità
Tutti i componenti e moduli implementano formalmente gli ID stabili definiti in `../shared/spec/screens.md`, `../shared/spec/features.md` e `../shared/spec/data-model.md`.
