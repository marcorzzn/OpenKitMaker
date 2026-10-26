# OpenKitMaker-Web

> **Target A+B: Svelte 5 + Vite 6 + PWA Offline-First**  
> **Autore:** [Marco Razzano](https://github.com/marcorzzn) (`@marcorzzn`)  
> **Licenza:** [MIT](../LICENSE)

Implementazione Web ad alte prestazioni del Design System ufficiale di **OpenKitMaker**.

---

## Architettura e Stack Tecnologico

- **Framework**: Svelte 5 con Runes (`$state`, `$derived`, `$effect`) su Vite 6 (Single Page Application pura, no overhead SSR).
- **Grafica 3D Hardware**: Three.js (WebGL2) caricato dinamicamente in lazy loading (chunk isolato `three-engine`) con PBR `MeshPhysicalMaterial`, texture fabric micro-normal procedurale, illuminazione duale *Studio* (6500K) / *Stadio* (5400K) e orbital camera.
- **Motore 2D Compositor**: Canvas 2048×2048 px ad alta precisione con supporto a pattern geometrici, colletti, polsini, loghi geometrici procedurali e testo dorsale curvato.
- **Storage Locale**: IndexedDB tramite Dexie.js (`OpenKitMakerWebDB`) per persistenza istantanea offline.
- **Esportazione Interoperabile**: Supporto nativo export PNG 2048×2048 e pacchetti `kit_<id>.zip` conformi allo standard universale OpenKitMaker.
- **PWA**: Service Worker (`public/sw.js`) con architettura Workbox per operatività 100% offline e installabilità desktop/mobile.

---

## Comandi Rapidi

```bash
# Installa dipendenze
npm install

# Avvia server di sviluppo (http://localhost:5173/)
npm run dev

# Build di produzione (bundle compresso in dist/)
npm run build

# Anteprima locale della build di produzione
npm run preview

# Verifica TypeScript
npm run lint
```

---

## Specifiche del Design System Implementate

Tutti i componenti e moduli implementano formalmente le specifiche stabili di OpenKitMaker:
- `SCR-HOME`: Dashboard di benvenuto con stato connettività e bozze recenti.
- `SCR-CATALOG`: Catalogo template con filtri a chip per categoria.
- `SCR-EDITOR`: Studio sartoriale con vista 2D, 3D e Split, selettore colore HSV & HEX e storico 8 colori.
- `SCR-SAVED`: Centro gestione kit salvati localmente (apri, rinomina, duplica, elimina, importa ZIP).
- `SCR-EXPORT`: Centro esportazione con anteprima duale, download PNG lossless e pacchetto ZIP firmato SHA-256.

---

## Autore

**Marco Razzano** — [@marcorzzn](https://github.com/marcorzzn)
