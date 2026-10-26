# OpenKitMaker — Schermate dell'Applicazione (Specifica di Parità)

Documento di specifica vincolante per le schermate di **OpenKitMaker** (Ramo Web e Ramo Native).
Ogni schermata possiede un identificativo stabile (**ID**) che deve essere implementato e referenziato nei sorgenti di entrambi i progetti (`// Implements: SCR-*`).

---

## Elenco Schermate e ID Stabili

| ID Schermata | Nome Funzionale | Descrizione / Ruolo | Navigazione Correlata |
|---|---|---|---|
| **`SCR-HOME`** | Landing & Dashboard | Schermata iniziale con panoramica, accesso rapido a catalogo template, ultimi kit modificati, stato offline | Entry point, naviga a `SCR-CATALOG` o `SCR-SAVED` |
| **`SCR-CATALOG`** | Catalogo Template | Griglia con filtri per categoria (Home, Away, Third, Portiere, Speciali) e motore di ricerca | Naviga a `SCR-EDITOR` con il template selezionato |
| **`SCR-EDITOR`** | Studio Editor Principale | Ambiente di creazione grafica: gestione layer, color picker, texture canvas 2D, viewport 3D interattivo | Accessibile da `SCR-CATALOG` o `SCR-SAVED`; naviga a `SCR-EXPORT` |
| **`SCR-SAVED`** | I Miei Kit (Archivio Locale) | Gestione bozze salvate localmente: lista kit, data modifica, anteprima 2D, azioni (duplica, rinomina, elimina) | Naviga a `SCR-EDITOR` per riprendere la modifica |
| **`SCR-EXPORT`** | Centro Esportazione | Configurazione esportazione texture 2048×2048 PNG e archivio interoperabile `kit_<id>.zip`, barra progresso | Triggerato da `SCR-EDITOR`; conferma e torna a `SCR-EDITOR` |

---

## Dettaglio Schermate

### 1. `SCR-HOME` — Dashboard
- **Header**: Logo OpenKitMaker, indicatore di stato connettività (Badge Offline / Online), pulsante impostazioni.
- **Hero Section**: Pulsante primario call-to-action *"Nuovo Kit da Template"* (`SCR-CATALOG`).
- **Sezione Kit Recenti**: Carosello orizzontale o griglia delle ultime 3 bozze modificate (anteprima minikit + nome + timestamp). Se vuoto, visualizza stato placeholder con invito alla creazione.
- **Accesso Rapido**: Pulsante *"Tutti i miei kit"* (`SCR-SAVED`).

### 2. `SCR-CATALOG` — Catalogo Template
- **Header**: Titolo schermata, pulsante di ritorno (`back`), campo di ricerca testuale per nome template/categoria.
- **Filtri di Categoria (Chips)**: Tutti, Casa (Home), Trasferta (Away), Terza (Third), Portiere (Goalkeeper), Speciali.
- **Griglia Template**: Card template con:
  - Immagine di anteprima 2D del kit base.
  - Nome del template e descrizione stilistica.
  - Badge categoria.
- **Interazione**: Tap/Click sulla card carica i layer predefiniti e instrada su `SCR-EDITOR`.

### 3. `SCR-EDITOR` — Editor Studio
L'area di lavoro principale è divisa in tre sezioni funzionali coordinate:
- **Top Navigation Bar**: Nome del kit editabile, pulsante Salva Bozza (`FUN-SAVE-LOCAL`), pulsante Esporta (`SCR-EXPORT`), toggle modalità vista (Solo 2D, Solo 3D, Split View).
- **Canvas / Viewport Area**:
  - **Anteprima 2D (`Texture2DView`)**: Visualizza il compositing a 2048×2048 (in scala) della texture distesa.
  - **Anteprima 3D (`Viewport3D`)**: Visualizza il modello 3D della maglia con texture PBR applicata, illuminazione configurabile (*Studio* / *Stadio*), orbita a 360° con pitch vincolato e zoom (pinch/wheel).
- **Sidebar / Bottom Panel (Layer & Color Controls)**:
  - **Icon Rail**: Selezione rapida componente (Base, Strisce, Colletto, Maniche, Sponsor, Nome, Numero).
  - **Layer Inspector**: Elenco livelli del componente attivo con toggle visibilità, slider opacità, blending mode.
  - **Color Picker**: Selettore cromatico HSV con input HEX, slider luminosità/saturazione e palette colori recenti.

### 4. `SCR-SAVED` — I Miei Kit
- **Header**: Titolo, pulsante ritorno, contatore kit salvati.
- **Griglia/Elenco Kit**: Card per ogni bozza persistita in locale:
  - Thumbnail 2D del minikit generato.
  - Nome kit, data e ora dell'ultimo salvataggio locale.
  - Menu azioni: *Apri nell'Editor*, *Duplica*, *Rinomina*, *Elimina*.
- **Dialog di Conferma**: Richiesto per l'eliminazione con avviso irreversibilità locale.

### 5. `SCR-EXPORT` — Centro Esportazione
- **Header**: Titolo e pulsante chiusura/annulla.
- **Anteprima**: Thumbnail della texture completa 2048×2048 e minikit 512×512.
- **Opzioni Esportazione**:
  - `PNG Alta Risoluzione`: Salva singola texture distesa 2048×2048 32-bit RGBA.
  - `Archivio Completo (.ZIP)`: Genera archivio interoperabile `kit_<id>.zip` contenente `texture.png`, `preview.png` e `manifest.json`.
- **Feedback Operativo**: Indicatore di avanzamento computazionale compositing/compressione e messaggio di successo con percorso di salvataggio (Download browser o cartella Documenti/Download Android).
