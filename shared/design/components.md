# OpenKitMaker — Anatomia dei Componenti UI (Specifica di Parità)

Documento di specifica per l'architettura dei componenti visuali in **OpenKitMaker**.
I componenti devono presentare la stessa struttura anatomica, gli stessi stati interattivi e la medesima gerarchia di proprietà su entrambe le piattaforme (Svelte 5 Components su Web, Jetpack Compose Composables su Android).

---

## 1. Componenti Base (Design System)

### `PrimaryButton` / `SecondaryButton`
- **Anatomia**: Icona opzionale a sinistra (20px), testo centrato, indicatore di caricamento circolare (spinner) integrato per azioni asincrone.
- **Stati**: Default, Hover/Pressed, Disabled, Loading.
- **Raggi**: Token `radii.md` (8px).
- **Tipografia**: `fontSize.base` (15px), `fontWeight.medium` (500).

### `ColorPickerControl`
- **Anatomia**:
  - Disco/Area 2D di selezione Tonalità/Saturazione (HSV).
  - Slider orizzontale Luminosità / Valore.
  - Box di visualizzazione colore attivo con codice HEX editabile.
  - Griglia a 8 tessere circolari per i colori recenti (tap rapido per applicazione).

### `LayerListItem`
- **Anatomia**:
  - Maniglia di trascinamento (drag handle per riordinamento z-index).
  - Icona tipologia componente (base, strisce, sponsor, numero).
  - Label con nome del layer.
  - Indicatore cromatico (cerchio colorato con il colore assegnato).
  - Toggle rapido visibilità (icona occhio aperto/chiuso).
  - Pulsante menu contestuale / opzioni avanzate (opacità, blend mode, elimina).
- **Stati**: Normale, Selezionato (bordo primario 2px), Trascinamento attivo.

---

## 2. Componenti di Dominio Complessi

### `TopNavBar`
- Barra di intestazione con:
  - Tasto indietro/chiudi.
  - Titolo kit modificabile con click diretto.
  - Gruppo pulsanti azione: *Salva Bozza*, *Vista (2D/3D/Split)*, *Esporta*.
- Altezza: 56dp / 56px fissa con `surface` background e bordo inferiore sottile (`surfaceBorder`).

### `Viewport3DContainer`
- Area contenitore per il rendering 3D hardware:
  - Canvas Three.js WebGL2 (Web) / SceneView Surface (Android).
  - Floating Action Button (FAB) in basso a destra per:
    - Reset orientamento camera (visuale frontale).
    - Toggle preset illuminazione (*Studio* / *Stadio*).
    - Toggle rotazione automatica (auto-spin).
  - Sovrimpressione di feedback per framerate (visibile solo in modalità debug/benchmark).

### `Texture2DView`
- Area di visualizzazione a schermo intero o split della texture piana compositata:
  - Griglia UV semi-trasparente opzionale per allineamento.
  - Funzione pinch-to-zoom e pan per ispezionare colletti e dettagli dei loghi ad alta risoluzione.

### `MinikitCard`
- Card usata in `SCR-HOME` e `SCR-SAVED`:
  - Contenitore con `surface` background e raggio `radii.lg` (16px).
  - Riquadro quadrato per la preview del minikit 2D.
  - Titolo del kit in grassetto (`fontWeight.bold`).
  - Data di salvataggio in testo secondario (`textSecondary`).
  - Barra azioni inferiore con pulsanti compatti: Modifica, Duplica, Cancella.
