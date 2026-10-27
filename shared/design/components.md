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

### `LayerListItem` / `LayerCard`
- **Anatomia**:
  - Contenitore Card con raggio `radii.md` (8px..16px), elevation token `elevation.level1`, background `surface` o `surfaceElevated`.
  - Padding interno standardizzato a `spacing.lg` (16dp / 16px) e spaziatura verticale `12dp..16dp`.
  - Maniglia di trascinamento (drag handle per riordinamento z-index).
  - Icona tipologia componente (base, trama, colletto, maniche, sponsor, stemma, nome, numero).
  - Label con nome del layer ed etichetta informativa (opacità, blend mode).
  - Indicatore cromatico (cerchio colorato con il colore assegnato).
  - Toggle rapido visibilità (icona occhio aperto/chiuso).
  - Pulsante menu contestuale / opzioni avanzate (opacità, blend mode, elimina).
- **Stati**: Normale, Selezionato (bordo primario 2px `primary`), Trascinamento attivo.

---

## 2. Componenti di Dominio Complessi

### `AppScaffold` & `ScreenScaffold`
- **Architettura Standard per Schermate**:
  - `topBar`: `TopAppBar` Material 3 (titolo schermata, azioni principali come *Salva* e *Preset Luce*).
  - `bottomBar`: `BottomAppBar` / `NavigationBar` ergonomica posta in basso, raggiungibile con il pollice su mobile (Colore, Livelli, Vista, Esporta).
  - `content`: Area flessibile e responsiva contenente il viewport (2D / 3D) e pannelli ispettore strutturati in `Card`.

### `TopNavBar` / `TopAppBar`
- Barra di intestazione con:
  - Tasto indietro/chiudi.
  - Titolo kit modificabile con click diretto o input rapido.
  - Gruppo pulsanti azione: *Salva Bozza*, *Preset Luce (Studio/Stadio)*, *Modalità Vista (2D/3D/Split)*.
- Altezza: 64dp fissa con `surface` background e bordo inferiore sottile (`surfaceBorder`).

### `BottomEditorBar` / `NavigationBar`
- Barra di controllo inferiore ancorata al fondo dello schermo (thumb-friendly per smartphone):
  - Scheda/Azione **Vista**: Toggle rapido 2D / 3D / Split.
  - Scheda/Azione **Colore**: Pannello / Sheet rapido per selezione HSV e palette colori recenti.
  - Scheda/Azione **Livelli**: Pannello gestore stack livelli e proprietà.
  - Scheda/Azione **Esporta**: Azione diretta per salvataggio e navigazione a esportazione.

### `Viewport3DContainer`
- Area contenitore per il rendering 3D hardware:
  - Canvas Three.js WebGL2 (Web) / SceneView Surface (Android).
  - Floating Action Button (FAB) in basso a destra per:
    - Reset orientamento camera (visuale frontale).
    - Toggle preset illuminazione (*Studio* / *Stadio*).
    - Toggle rotazione automatica (auto-spin).
  - Sovrimpressione di feedback per framerate (visibile solo in modalità debug/benchmark).

### `Jersey2DMockup` & `Texture2DView`
- **Mockup Maglia 2D (Default)**:
  - Presenta un mockup realistico della maglia (sagome Fronte e Retro affiancate) anziché il foglio UV srotolato.
  - Mascheratura precisa sulle sagome sartoriali (torso, maniche raglan inclinate, colletto a costine).
  - Ombreggiatura realistica sottostante (drop shadow multi-layer `rgba(0,0,0,0.55)` con sfocatura 24px/offset Y).
  - Sfumatura volumetrica 3D sul tessuto per eliminare l'effetto piatto ("naïf").
  - Switcher rapido: `[ Maglia 2D (Mockup) | Foglio UV (Stampa) ]` per consentire ai modder di ispezionare il foglio 2048×2048 se necessario.
- **Mappatura UV Maniche (Texture Sheet 2048×2048)**:
  - Manica Sinistra: `x: 0..680`, `y: 1360..2048` (rapporto normalizzato `0.0..0.332 w`, `0.664..1.0 h`).
  - Manica Destra: `x: 1024..1704`, `y: 1360..2048` (`0.50..0.832 w`, `0.664..1.0 h`).
  - Colletto: `x: 680..1024`, `y: 1360..2048` (`0.332..0.50 w`, `0.664..1.0 h`).
  - Bordi e Trim: `x: 1704..2048`, `y: 1360..2048` (`0.832..1.0 w`, `0.664..1.0 h`).

### `TopNavBar` / `TopAppBar` (Compatta)
- Intestazione ottimizzata per smartphone (altezza compatta 48dp..52dp):
  - Titolo sintetico: "Kit Maker".
  - Navigazione: Icone compatte e menu a tendina (Dropdown) per Home, Catalogo, I Miei Kit per azzerare l'overflow di testo.
  - Riga nome kit: Campo compatto su una sola riga (altezza 36dp..40dp, `singleLine = true`).
  - Pulsanti azione compatti: Preset Luce (Studio/Stadio) e Salva.

### `MinikitCard`
- Card usata in `SCR-HOME` e `SCR-SAVED`:
  - Contenitore con `surface` background e raggio `radii.lg` (16px).
  - Riquadro quadrato per la preview del minikit 2D con maniche e colletto visibili.
  - Titolo del kit in grassetto (`fontWeight.bold`).
  - Data di salvataggio in testo secondario (`textSecondary`).
  - Barra azioni inferiore con pulsanti compatti: Modifica, Duplica, Cancella.


