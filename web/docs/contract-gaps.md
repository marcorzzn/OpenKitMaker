# Analisi Gap del Contratto di Parità (`CONTEXT_OpenKitMaker.md`) e Soluzioni Adottate

> **Prodotto:** `OpenKitMaker-Web` (Target A+B: Svelte 5 + Vite + PWA)  
> **Riferimento Contratto:** `CONTEXT_OpenKitMaker.md` v1.0.0

Durante l'ingegnerizzazione dell'architettura di `OpenKitMaker-Web`, sono state rilevate alcune omissioni o ambiguità formali nel documento di specifica `CONTEXT_OpenKitMaker.md`. In conformità alle Regole di Condotta, ogni gap è documentato di seguito insieme alla soluzione deterministica implementata per garantire la piena interoperabilità con `OpenKitMaker-Native` (Target C - Android).

---

## GAP-01: Definizione Formale di `KitMetadata`, `Decal` e `Template`
### Problema Rilevato
- Nella Sezione 1 (*La Regola di Parità*) vengono citate le entità condivise `Kit`, `Layer`, `Decal`, `Template`.
- Nella Sezione 5 (*Modello Dati Comune*) l'interfaccia `Kit` referenzia `metadata: KitMetadata`, ma né `KitMetadata`, né `Decal`, né `Template` sono esplicitate nel blocco TypeScript.

### Soluzione Proposta e Implementata (`src/types/kit.ts`)
Sono state definite interfacce rigorose e serializzabili 1:1 con Kotlin (`kotlinx.serialization` / Room):
```typescript
export interface KitMetadata {
  author: string;
  version: string;
  description?: string;
  tags: string[];
  thumbnailDataUrl?: string; // Minikit 512x512 in base64 o data URL per SCR-HOME / SCR-SAVED
  finishType: 'MATTE_MESH' | 'PRO_VAPOR' | 'RETRO_SATIN';
  lightingPreset: 'studio' | 'stadium';
}

export interface Decal {
  id: string;
  shape: 'HEXAGON' | 'SHIELD' | 'DIAMOND' | 'CIRCLE' | 'CHEVRON' | 'STAR_8';
  u: number;        // 0.0 .. 1.0 coordinate UV normalizzate
  v: number;        // 0.0 .. 1.0 coordinate UV normalizzate
  scale: number;    // 0.1 .. 2.0
  rotation: number; // -180 .. 180 gradi
  color: string;
  strokeColor: string;
}

export interface Template {
  id: string;
  name: string;
  category: KitCategory;
  description: string;
  accentPreviewColor: string;
  basePreviewColor: string;
  patternSummary: string;
  defaultLayers: Omit<Layer, 'id'>[];
}
```

---

## GAP-02: Tipizzazione di `Layer.properties` (`Record<string, any>`)
### Problema Rilevato
Il contratto definisce `properties: Record<string, any>` per l'entità `Layer` e richiede in `FUN-COMPOSITE-2D` e `TEST-PAR-06` che un archivio `kit_<id>.zip` importato tra Web e Android produca un rendering 2048×2048 con $\Delta E < 1.0$. Senza uno schema deterministico per `properties`, i due motori 2D non possono riprodurre identicamente pattern geometrici, testi curvati e numeri con contorno.

### Soluzione Proposta e Implementata (`src/types/kit.ts` & `src/lib/compositor2d.ts`)
È stato formalizzato `LayerProperties` con chiavi tipizzate per ciascun `LayerType`:
- **`PATTERN`**: `patternStyle` (`'VERTICAL_STRIPES' | 'HORIZONTAL_HOOPS' | 'DIAGONAL_SASH' | 'CHEVRON_BREAST' | 'HALVES' | 'CHECKERED_GEOMETRIC' | 'CROSS_GRID' | 'PINSTRIPES' | 'RADIAL_RAYS'`), `stripeCount` (2..16), `stripeWidth` (0.1..0.9), `angle` (-90..90).
- **`COLLAR`**: `collarStyle` (`'ROUND_CREW' | 'V_NECK' | 'POLO_FOLDED' | 'MANDARIN_TRIM'`), `trimWidth` (4..32).
- **`SLEEVES`**: `cuffWidth` (10..60), `raglanCut` (`boolean`).
- **`SPONSOR`**: `sponsorGeometry` (`'GEOMETRIC_WORDMARK' | 'HEX_TECH_BAR' | 'TRIPLE_DIAMOND' | 'ABSTRACT_WAVE'`), `text` (stringa opzionale geometrica), `scale` (0.5..1.5), `offsetY` (-200..200).
- **`BADGE`**: `badgeShape` (`'SHIELD_CLASSIC' | 'HEXAGON_PRO' | 'CIRCLE_MONO' | 'DIAMOND_CREST'`), `position` (`'LEFT_CHEST' | 'CENTER_CHEST' | 'RIGHT_CHEST'`), `scale` (0.5..1.5).
- **`TEXT_NAME`**: `text` (es. `"CAPITANO"`), `curveRadius` (0 = dritto, 400..1600 = raggio arco superiore in px a 2048×2048), `fontSize` (40..140), `strokeWidth` (0..16), `offsetY` (-150..150).
- **`NUMBER`**: `numberText` (es. `"10"`), `fontSize` (180..480), `strokeWidth` (0..24), `showFrontNumber` (`boolean`), `innerLineOffset` (0..12).

---

## GAP-03: Allineamento tra Icon Rail (`SCR-EDITOR`) e `LayerType` (`data-model.md`)
### Problema Rilevato
Nella descrizione testuale di `SCR-EDITOR` (§3) sono elencati 7 componenti nell'Icon Rail (*Base, Strisce, Colletto, Maniche, Sponsor, Nome, Numero*), mentre la definizione `LayerType` (§5) prevede 8 tipi (`BASE`, `PATTERN`, `COLLAR`, `SLEEVES`, `SPONSOR`, `BADGE`, `TEXT_NAME`, `NUMBER`).

### Soluzione Proposta e Implementata (`src/components/ScreenEditor.svelte`)
L'Icon Rail in `SCR-EDITOR` espone tutti gli 8 `LayerType` del modello dati (includendo `BADGE` / Stemma geometrico sul petto, oltre a `PATTERN` / Strisce) così che nessun layer definito nel `manifest.json` rimanga privo di interfaccia di ispezione. Inoltre l'utente può aggiungere, riordinare (z-index su/giù), duplicare o eliminare livelli (`FUN-MANAGE-LAYERS`).

---

## GAP-04: UV Atlas Layout Standardizzato a 2048×2048 per Parità 2D/3D
### Problema Rilevato
Il contratto richiede una texture `2048×2048` visualizzabile sia distesa in `Texture2DView` sia proiettata sulla mesh 3D in `Viewport3D`, ma non specifica la mappa di coordinate UV dei quadranti sartoriali.

### Soluzione Proposta e Implementata (`src/lib/compositor2d.ts` & `src/lib/engine3d.ts`)
È stato definito un **OpenKitMaker Standard UV Atlas (2048×2048)**:
| Quadrante | Regione Pixel (2048×2048) | Range UV ($u, v \in [0,1]$) | Componente Sartoriale |
| :--- | :--- | :--- | :--- |
| **Top-Left** | `x: 0..1024, y: 0..1360` | $u \in [0.0, 0.5], v \in [0.336, 1.0]$ | **Torso Frontale** (Base, Pattern, Sponsor, Badge, Numero Frontale) |
| **Top-Right** | `x: 1024..2048, y: 0..1360` | $u \in [0.5, 1.0], v \in [0.336, 1.0]$ | **Torso Posteriore** (Base, Pattern, Nome Curvato, Numero Dorsale) |
| **Bottom-Left** | `x: 0..1024, y: 1360..2048` | $u \in [0.0, 0.5], v \in [0.0, 0.336]$ | **Manica Sinistra, Polsino SX & Colletto Anteriore** |
| **Bottom-Right** | `x: 1024..2048, y: 1360..2048` | $u \in [0.5, 1.0], v \in [0.0, 0.336]$ | **Manica Destra, Polsino DX & Colletto Posteriore / Trim** |

La mesh 3D della maglia in `src/lib/engine3d.ts` mappa esattamente i propri vertici UV su questi 4 quadranti, assicurando corrispondenza pixel-perfect tra il Canvas 2D e il modello 3D PBR.

---

## GAP-05: Entry Point UI per `TEST-PAR-06` (Cross-Platform ZIP Import)
### Problema Rilevato
Lo scenario `TEST-PAR-06` richiede l'importazione di un archivio `kit_<id>.zip` con validazione del `manifest.json` e ricomposizione della texture a 2048×2048, ma le specifiche delle 5 schermate non indicano dove posizionare il controllo di importazione.

### Soluzione Proposta e Implementata
È stata implementata la funzione `importKitFromZip(file: File)` in `src/lib/exporter.ts` (con verifica integrità SHA-256 di `texture.png` e `preview.png` tramite Web Crypto API e validazione schema `manifest.json` v1.0.0) ed è stato reso disponibile il pulsante **"Importa ZIP (.zip)"** sia in `SCR-HOME`, sia in `SCR-SAVED`, sia in `SCR-EXPORT`.

---

## GAP-06: Pipeline KTX2 / Draco in Modalità 100% Offline (`FUN-OFFLINE-MODE`)
### Problema Rilevato
Il contratto richiede `KTX2Loader` e `DRACOLoader` in Three.js e al contempo il funzionamento 100% offline (`TEST-PAR-07`) senza dipendenze esterne fragili.

### Soluzione Proposta e Implementata (`src/lib/engine3d.ts`)
Il modulo `src/lib/engine3d.ts` (caricato esclusivamente via `import('./lib/engine3d')` quando l'utente attiva la vista `3D` o `SPLIT` per rispettare il limite di bundle iniziale $\le 80\text{ KB}$ gzip) inizializza `KTX2Loader` e `DRACOLoader` con supporto di caching Workbox nel Service Worker e genera proceduralmente:
1. Una mesh sartoriale 3D multi-parte ad alta densità (Torso anatomico, Maniche raglan, Colletto sagomato, Polsini, Orlo inferiore, Stand manichino tecnico).
2. Normal map e Roughness map PBR micro-fibra traspirante in memoria, combinate con la `CanvasTexture` 2048×2048 aggiornata in tempo reale dal compositore 2D.
