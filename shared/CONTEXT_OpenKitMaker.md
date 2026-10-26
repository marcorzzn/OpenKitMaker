# CONTEXT_OpenKitMaker: Documento Unificato di Contesto e Parità

> **Documento Master di Riferimento per Google AI Studio e Sviluppo Parallelo**  
> **Nome Progetto:** OpenKitMaker  
> **Rami di Sviluppo:** `OpenKitMaker-Web` (Target A+B: Svelte 5 + Vite + PWA/TWA) e `OpenKitMaker-Native` (Target C: Kotlin + Compose + Filament)  
> **Principio Fondamentale:** *Un Prodotto, Due Target, Un Contratto di Parità.*

---

## 1. Visione e Vincolo di Parità

OpenKitMaker è uno studio grafico e di modellazione 3D dedicato alla creazione, personalizzazione, salvataggio locale ed esportazione di divise sportive (kit).
Il progetto viene sviluppato simultaneamente su due target complementari:
1. **Target Web (A+B)**: PWA ad alte prestazioni realizzata con **Svelte 5 (Runes)**, Vite, Three.js (WebGL2) e impacchettamento TWA (Trusted Web Activity) per Android tramite Bubblewrap.
2. **Target Native (C)**: Applicazione nativa Android realizzata con **Kotlin 2.x**, **Jetpack Compose (Material 3)**, **Google Filament / SceneView (Vulkan / OpenGL ES)** e Room SQLite.

### La Regola di Parità
Entrambi i target implementano rigorosamente lo stesso contratto:
- **Stesse Schermate**: ID stabili `SCR-HOME`, `SCR-CATALOG`, `SCR-EDITOR`, `SCR-SAVED`, `SCR-EXPORT`.
- **Stesse Funzionalità**: ID stabili `FUN-SELECT-TEMPLATE`, `FUN-MANAGE-LAYERS`, `FUN-EDIT-COLOR`, `FUN-COMPOSITE-2D`, `FUN-PREVIEW-3D`, `FUN-SAVE-LOCAL`, `FUN-EXPORT-PNG`, `FUN-EXPORT-ZIP`, `FUN-OFFLINE-MODE`.
- **Stesso Data Model**: Entità e tipi identici (`Kit`, `Layer`, `Decal`, `Template`).
- **Interoperabilità Totale**: Un archivio `kit_<id>.zip` esportato dal Web deve essere apribile da Android e viceversa con identico rendering a 2048×2048 pixel.
- **Nessun Marchio Protetto di Terzi**: Usare esclusivamente forme geometriche, pattern vettoriali astratti e placeholder procedurali; nessun logo coperto da copyright (es. marchi sportivi, sponsor commerciali o club reali).

---

## 2. Sintesi dei Vincoli Prestazionali (Derivati dall'Audit)

1. **Rete (Wire Size)**: Le texture non compresse dominano la rete (68,1% del traffico). Tutte le texture 3D devono essere distribuite in formato GPU block-compressed **KTX2 / Basis Universal** (ASTC 6×6 per mobile, BC7 per desktop) con risparmio su rete del 60–75% e decodifica istantanea hardware.
2. **Memoria (In-Memory AST / VRAM)**:
   - Una texture PNG 2048×2048 alloca 16,7 MB di VRAM non compressa; KTX2 riduce la VRAM a 4,1 MB (-75%).
   - Il bundle JS iniziale del web deve rimanere **≤ 80 KB gzip**; Three.js e i moduli pesanti devono essere caricati in modalità lazy solo quando l'utente attiva il viewport 3D.
3. **Priorità #1 su Android (Sblocco Funzionale GPU)**:
   - La baseline Kotlin esistente eseguiva una proiezione software 3D in CPU su `Canvas.drawPath` con illuminazione Gouraud, degradando il framerate a `< 20 FPS`.
   - **È fatto divieto assoluto di usare Canvas software per la proiezione 3D**. Il rendering 3D deve essere gestito dalla GPU tramite **Google Filament / SceneView**, garantendo **60 FPS stabili** (frame time ≤ 16,6 ms) su dispositivi fisici mid-range.

---

## 3. Specifica Schermate (ID Stabili `SCR-*`)

### `SCR-HOME`: Dashboard
- Header con logo OpenKitMaker e indicatore di connettività (Badge Offline / Online).
- Call-to-action principale: "Nuovo Kit da Template" -> `SCR-CATALOG`.
- Carosello o griglia delle ultime 3 bozze modificate (minikit 2D + nome + data).
- Accesso rapido: "Tutti i miei kit" -> `SCR-SAVED`.

### `SCR-CATALOG`: Catalogo Template
- Barra di ricerca e filtri categorie a chip: Tutti, Casa, Trasferta, Terza, Portiere, Speciali.
- Griglia card template con anteprima 2D, nome e badge categoria.
- Tap su template clona la configurazione e apre `SCR-EDITOR`.

### `SCR-EDITOR`: Studio di Creazione Grafica
- **Top Navigation Bar**: Titolo kit editabile, Salva Bozza, Esporta, selettore vista (2D, 3D, Split).
- **Canvas 2D (`Texture2DView`)**: Visualizzazione ad alta fedeltà della texture distesa (risoluzione interna 2048×2048).
- **Viewport 3D (`Viewport3D`)**: Visualizzazione del modello 3D della maglia con texture PBR, illuminazione Studio/Stadio, rotazione orbitale 360° e zoom con pitch vincolato.
- **Pannello Livelli & Colori**:
  - Icon rail per componente (Base, Strisce, Colletto, Maniche, Sponsor, Nome, Numero).
  - Layer inspector con slider opacità (0.0..1.0), visibilità, blend mode (NORMAL, MULTIPLY, OVERLAY).
  - Color picker HSV con codice HEX e palette colori recenti.

### `SCR-SAVED`: I Miei Kit
- Elenco kit persistiti in memoria locale (IndexedDB / Room).
- Card minikit con thumbnail, data ultimo salvataggio.
- Azioni: Apri nell'Editor, Rinomina, Duplica, Elimina con dialog di conferma.

### `SCR-EXPORT`: Centro Esportazione
- Anteprima texture distesa 2048×2048 e minikit 512×512.
- Scelta output:
  - **PNG 2048×2048**: Salvataggio texture ad alta definizione.
  - **Archivio ZIP Interoperabile**: File `kit_<id>.zip` contenente `texture.png`, `preview.png`, `manifest.json`.
- Barra di avanzamento compositing/compressione e notifica di completamento.

---

## 4. Specifica Funzionalità (ID Stabili `FUN-*`)

- **`FUN-SELECT-TEMPLATE`**: Inizializza nuovo Kit clonando layer base di un template con nuovo UUIDv4.
- **`FUN-MANAGE-LAYERS`**: Operazioni CRUD, ordinamento z-index, opacità (0.0..1.0), visibilità e blend mode.
- **`FUN-EDIT-COLOR`**: Aggiornamento reattivo immediato colore in formato HEX (`#RRGGBB` o `#RRGGBBAA`) e gestione storico ultimi 8 colori.
- **`FUN-COMPOSITE-2D`**: Motore grafico che compone i layer su canvas/bitmap 2048×2048 a 32-bit RGBA (supporto testo curvato e numeri vettoriali con contorno).
- **`FUN-PREVIEW-3D`**: Proiezione texture su mesh 3D maglia PBR con camera orbitale vincolata e preset luce Studio/Stadio.
- **`FUN-SAVE-LOCAL`**: Persistenza bozza su IndexedDB (Web) o Room (Android) con generazione minikit 512×512.
- **`FUN-EXPORT-PNG`**: Generazione e download texture PNG lossless 2048×2048.
- **`FUN-EXPORT-ZIP`**: Confezionamento archivio standard `kit_<id>.zip` interoperabile.
- **`FUN-OFFLINE-MODE`**: Garanzia di funzionamento offline al 100% per tutte le operazioni di editing, anteprima e salvataggio locale.

---

## 5. Modello Dati Comune (`data-model.md`)

```typescript
interface Kit {
  id: string;                    // UUID v4
  name: string;                  // Nome kit
  category: KitCategory;         // "HOME" | "AWAY" | "THIRD" | "GK" | "SPECIAL"
  createdAt: string;             // ISO-8601 UTC
  updatedAt: string;             // ISO-8601 UTC
  templateId: string;            // ID template base
  layers: Layer[];               // Array ordinato dal basso verso l'alto
  metadata: KitMetadata;
}

type KitCategory = "HOME" | "AWAY" | "THIRD" | "GK" | "SPECIAL";

interface Layer {
  id: string;                    // UUID univoco
  name: string;                  // Nome visualizzato
  type: LayerType;               // Tipologia componente
  visible: boolean;
  opacity: number;               // 0.0 .. 1.0
  zIndex: number;
  color: string;                 // "#RRGGBB" o "#RRGGBBAA"
  secondaryColor?: string;
  blendMode: "NORMAL" | "MULTIPLY" | "OVERLAY";
  properties: Record<string, any>;
}

type LayerType = "BASE" | "PATTERN" | "COLLAR" | "SLEEVES" | "SPONSOR" | "BADGE" | "TEXT_NAME" | "NUMBER";
```

Mappatura Kotlin corrispondente in `data class Kit`, `data class Layer`, `enum class KitCategory`, `enum class LayerType`.

---

## 6. Formato Esportazione e Manifest (`export-format.md`)

Struttura dello ZIP:
```text
kit_<id>.zip
├── texture.png            # 2048×2048 RGBA 32-bit lossless
├── preview.png            # 512×512 RGBA
└── manifest.json          # Schema formale interoperabile
```

Schema `manifest.json`:
```json
{
  "$schema": "https://openkitmaker.dev/schemas/kit-manifest-v1.json",
  "version": "1.0.0",
  "kitId": "UUID",
  "name": "Nome Kit",
  "category": "HOME",
  "exportTimestamp": "2026-10-26T20:00:00Z",
  "generator": {
    "platform": "OpenKitMaker-Web",
    "version": "1.0.0",
    "buildNumber": "100"
  },
  "dimensions": { "width": 2048, "height": 2048 },
  "checksums": { "textureSha256": "...", "previewSha256": "..." },
  "kitData": {
    "templateId": "...",
    "layers": []
  }
}
```

---

## 7. Design Tokens (`tokens.json` in formato compatto)

```json
{
  "color": {
    "primary": "#0F52BA",
    "primaryVariant": "#0A387E",
    "secondary": "#00C49F",
    "background": "#0F141C",
    "surface": "#1A2230",
    "surfaceElevated": "#253146",
    "surfaceBorder": "#2E3D56",
    "textPrimary": "#FFFFFF",
    "textSecondary": "#94A3B8",
    "textTertiary": "#64748B",
    "error": "#EF4444",
    "success": "#10B981",
    "warning": "#F59E0B"
  },
  "spacing": { "xs": "4px", "sm": "8px", "md": "12px", "lg": "16px", "xl": "24px", "xxl": "32px" },
  "radii": { "sm": "4px", "md": "8px", "lg": "16px", "full": "9999px" },
  "motion": { "durationNormal": "250ms", "easingStandard": "cubic-bezier(0.4, 0.0, 0.2, 1)" },
  "lighting": {
    "studio": { "intensity": 1.2, "colorTemp": 6500 },
    "stadium": { "intensity": 1.8, "colorTemp": 5400 }
  }
}
```

---

## 8. Chiavi di Localizzazione i18n (`it.json` principali)

- `app.name`: "OpenKitMaker"
- `app.offline_badge`: "Modalità Offline"
- `nav.home`: "Home", `nav.catalog`: "Catalogo", `nav.editor`: "Editor Kit", `nav.saved`: "I Miei Kit", `nav.export`: "Esporta"
- `catalog.title`: "Catalogo Template"
- `editor.view_2d`: "Vista 2D", `editor.view_3d`: "Vista 3D", `editor.view_split`: "Vista Doppia"
- `export.option_png_title`: "Texture PNG 2048×2048", `export.option_zip_title`: "Archivio Completo (.ZIP)"
- `errors.storage_full`: "Spazio di archiviazione locale insufficiente per completare l'operazione."
- `errors.export_failed`: "Si è verificato un errore durante l'esportazione del file. Riprova."

---

## 9. Scenari di Test per la Parità (`parity-tests.md`)

- **`TEST-PAR-01`**: Template Selection & Load (apertura in ≤ 1,5s con layer clonati).
- **`TEST-PAR-02`**: Layer Color Mutex & 2D Update (aggiornamento canvas entro 16,6 ms).
- **`TEST-PAR-03`**: 3D Viewport Orbit & Framerate (rotazione 360° per 10s a 60 FPS stabili, frame ≤ 16,6 ms).
- **`TEST-PAR-04`**: Local Persistence & Reload (salva e ricarica identica da storage locale).
- **`TEST-PAR-05`**: Interoperable Export ZIP (genera `kit_<id>.zip` con texture 2048×2048 e manifest valido).
- **`TEST-PAR-06`**: Cross-Platform ZIP Import (Web importa ZIP Android e viceversa con DeltaE visivo < 1.0).
- **`TEST-PAR-07`**: Offline Autonomy (funzionamento al 100% in assenza di rete).
