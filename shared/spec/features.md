# OpenKitMaker — Funzionalità e Contratti Operativi (Specifica di Parità)

Documento di specifica funzionale per **OpenKitMaker**.
Ogni funzione possiede un identificativo stabile (**ID**) che deve essere referenziato nei sorgenti di entrambi i progetti (`// Implements: FUN-*`).

---

## Tabella Funzionalità e ID Stabili

| ID Funzione | Nome | Descrizione Sintetica | Schermata Associata |
|---|---|---|---|
| **`FUN-SELECT-TEMPLATE`** | Selezione Template | Inizializza il kit clonando la configurazione base di un template predefinito | `SCR-CATALOG` |
| **`FUN-MANAGE-LAYERS`** | Gestione Livelli | Aggiunge, riordina, elimina, nasconde o modifica l'opacità dei layer grafici | `SCR-EDITOR` |
| **`FUN-EDIT-COLOR`** | Selettore Colore | Modifica il colore primario/secondario di un layer con aggiornamento reattivo immediato | `SCR-EDITOR` |
| **`FUN-COMPOSITE-2D`** | Compositing Texture 2D | Motore grafico che compone i layer vettoriali e raster su texture 2048×2048 | `SCR-EDITOR`, `SCR-EXPORT` |
| **`FUN-PREVIEW-3D`** | Render PBR 3D | Renderizza la maglia 3D con camera orbitale, zoom, illuminazione Studio/Stadio | `SCR-EDITOR` |
| **`FUN-SAVE-LOCAL`** | Salvataggio Locale | Salva lo stato completo del kit su storage locale (IndexedDB / Room) | `SCR-EDITOR`, `SCR-SAVED` |
| **`FUN-EXPORT-PNG`** | Esportazione PNG | Genera ed esporta la texture finale 2048×2048 RGBA non decompressa con perdita | `SCR-EXPORT` |
| **`FUN-EXPORT-ZIP`** | Esportazione ZIP Kit | Confeziona `kit_<id>.zip` contenente texture, minikit e `manifest.json` standard | `SCR-EXPORT` |
| **`FUN-OFFLINE-MODE`** | Funzionamento Offline | Garantisce l'esecuzione completa delle funzionalità di editing e salvataggio senza connessione | Tutto il sistema |

---

## Specifiche Dettagliate delle Funzioni

### `FUN-SELECT-TEMPLATE`
- **Trigger**: Selezione di un template dalla griglia `SCR-CATALOG`.
- **Comportamento**:
  1. Istanzia un nuovo oggetto `Kit` con ID univoco UUIDv4.
  2. Assegna al kit il nome del template con suffisso progressivo se già esistente.
  3. Clona l'elenco dei layer base (corpo, colletto, maniche, pattern) associati al template.
  4. Genera immediatamente il primo compositing 2D e aggiorna lo stato dell'editor.

### `FUN-MANAGE-LAYERS`
- **Operazioni ammesse**:
  - `ToggleVisibility(layerId)`: Mostra/nasconde il layer nel compositing.
  - `SetOpacity(layerId, float value)`: Variazione continua tra `0.0` (trasparente) e `1.0` (opaco).
  - `Reorder(fromIndex, toIndex)`: Spostamento del layer lungo l'asse z-index.
  - `SetBlendMode(layerId, mode)`: Supporto per `NORMAL`, `MULTIPLY`, `OVERLAY`.
- **Garanzia**: Ogni mutazione scatena l'invalidazione della texture 2D (`FUN-COMPOSITE-2D`) e il reload della texture nel viewport 3D (`FUN-PREVIEW-3D`) entro 16 ms.

### `FUN-EDIT-COLOR`
- **Input**: Valore HEX string (`#RRGGBB` o `#RRGGBBAA`) oppure valori HSV (`h: 0..360, s: 0..1, v: 0..1`).
- **Comportamento**:
  - Aggiorna in tempo reale la proprietà `color` del layer target.
  - Mantiene uno storico degli ultimi 8 colori usati per riuso rapido.
  - Supporta palette preset (Colori base club, colori fluo, tonalità neutre).

### `FUN-COMPOSITE-2D`
- **Requisiti di Rendering**:
  - Risoluzione interna standard: **2048×2048 pixel**, canale colore a 32-bit RGBA.
  - Supporto coordinate UV standard per kit sportivi (aree mappate: fronte busto, retro busto, manica destra, manica sinistra, colletto).
  - Supporto per rendering tipografico: testi dorsali con curvatura su arco (`arcText`) e numeri bicolore con contorno vettoriale (`outline`).
  - Implementazione Web: `OffscreenCanvas` / Canvas2D con worker dedicato.
  - Implementazione Android: `Bitmap` hardware con `Canvas` vettoriale (ereditato da `TextureEngine.kt`).

### `FUN-PREVIEW-3D`
- **Modello Geometrico**: Mesh maglia suddivisa in submesh per materiali distinti (jersey_body, collar, sleeves).
- **Illuminazione**:
  - *Preset Studio*: Luce IBL neutra bilanciata a 3 punti per massima leggibilità dei dettagli.
  - *Preset Stadio*: Luce IBL con dominante calda/fredda e ombre direzionali marcate.
- **Interazione Camera**:
  - Rotazione orizzontale libera (360°).
  - Inclinazione verticale limitata (pitch tra -30° e +45° per evitare disorientamento).
  - Distanza / Zoom vincolata tra `minDistance` e `maxDistance`.

### `FUN-SAVE-LOCAL`
- **Destinazione**:
  - Web: `Dexie.js` su IndexedDB (`kit_database`).
  - Native: `Room Database` (`kit_database.sqlite`).
- **Trigger**:
  - Salvataggio esplicito tramite pulsante Salva.
  - Auto-salvataggio su transizione di schermata o perdita di focus.
- **Snapshot generato**: Ad ogni salvataggio viene generato un thumbnail compresso 512×512 (minikit) per la visualizzazione nella lista `SCR-SAVED`.

### `FUN-EXPORT-PNG` e `FUN-EXPORT-ZIP`
- **Standard di interoperabilità**: Un archivio esportato da `OpenKitMaker-Web` deve poter essere importato e riaperto su `OpenKitMaker-Native` e viceversa senza alcuna perdita informativa.
- Vedere [export-format.md](file:///c:/Progetti/OpenKit%20Maker/shared/spec/export-format.md) per lo schema rigido dei file e del manifest.

### `FUN-OFFLINE-MODE`
- L'applicazione non deve mai fallire né visualizzare schermate di blocco in assenza di connessione internet.
- I template predefiniti sono distribuiti localmente (nel bundle web o negli asset APK).
