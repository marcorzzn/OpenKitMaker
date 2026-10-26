# Manuale Operativo di Audit, Analisi Architetturale e Porting Android
## Target: `fifakitcreator.com` — Strumento di Analisi: REA MCP Server (Browser CDP Provider)

---

### Inquadramento di Conformità, Sessione Anonima ed Esclusione Proprietà Intellettuale

> [!IMPORTANT]
> **Dichiarazione su Sessione Anonima e Trattamento Dati (GDPR):**  
> L'audit è stato eseguito su sessione Google Chrome anonima con profilo temporaneo isolato (`$env:TEMP\rea_audit_profile`). Nessun token di autenticazione, dato personale o contenuto di area riservata è stato acquisito o conservato. L'analisi è limitata alle risorse e configurazioni 3D pubblicamente erogate dall'applicazione.

> [!WARNING]
> **Avvertenza sulla Proprietà Intellettuale e Marchi di Terzi:**  
> Il porting Android è rigorosamente limitato all'architettura software e alle funzionalità del motore grafico. Gli asset grafici di terze parti rilevati durante l'audit (marchi sportivi come Adidas, branding FIFA/EA Sports, stemmi e logotipi di club) sono protetti da copyright e marchi registrati, rimangono soggetti alle rispettive licenze d'uso e **non costituiscono oggetto di porting o ridistribuzione**. Il committente si assume la piena ed esclusiva responsabilità circa la titolarità dei contenuti caricati o distribuiti tramite l'applicazione finale.

---

### 1. Inquadramento Metodologico e Conformità
Questo manuale guida il consulente tecnico nell'esecuzione dell'audit architetturale e prestazionale del sito **fifakitcreator.com** su formale incarico del committente (titolare del sito).

**Linee guida operative:**
- **Approccio Passivo**: Nessuna iniezione di codice, nessuna manipolazione del backend o alterazione dello stato. L'analisi si basa sull'osservazione via Chrome DevTools Protocol (CDP) e sul parsing statico dei bundle pubblici.
- **Tracciabilità delle Evidenze**: Tutte le osservazioni vengono registrate in formati aperti (CSV, schemi JSON, report Markdown) e consolidate tramite il *Deterministic Evidence Bundle* di REA ([`fifakitcreator_evidence_bundle.rea.zip`](file:///c:/Progetti/OpenKit%20Maker/fifakitcreator_evidence_bundle.rea.zip)).
- **Destinazione d'Uso**: Ottimizzazione delle performance web e supporto alla progettazione del porting su applicazione nativa Android (progetto **OpenKit Studio** presente in [`openkit-studio.zip`](file:///c:/Progetti/OpenKit%20Maker/openkit-studio.zip)).
- **Nota sulla Riproducibilità del Notebook**: L'hash SHA-256 di `notebook_audit_fifakitcreator.ipynb` riflette il file nello stato di consegna congelato; un'eventuale riesecuzione con salvataggio in ambiente Jupyter/Colab ne aggiorna i metadati e i buffer di output interni, senza alterare la deterministica riproducibilità degli altri cinque artefatti generati.

---

### 2. Configurazione dell'Ambiente Operativo con REA

#### 2.1 Architettura dell'interazione
```
+------------------------------------+
|  Browser Target (Google Chrome)    |
|  https://fifakitcreator.com/       |
|  CDP Listener: 127.0.0.1:9222      |
+-----------------+------------------+
                  |
                  | Chrome DevTools Protocol (CDP)
                  v
+------------------------------------+
|  REA (Reverse Engineer Anything)   |
|  Server MCP & Motore di Analisi    |
+-----------------+------------------+
                  |
                  | MCP (Model Context Protocol) / JSON-RPC
                  v
+------------------------------------+
|  Antigravity IDE / Notebook Python |
|  Quaderno di Audit & Report Maker  |
+------------------------------------+
```

#### 2.2 Avvio del Browser con porta CDP
Eseguire da **PowerShell**:
```powershell
& "C:\Program Files\Google\Chrome\Application\chrome.exe" `
    --remote-debugging-port=9222 `
    --user-data-dir="$env:TEMP\rea_audit_profile" `
    https://fifakitcreator.com/
```
*Nota per Command Prompt (`cmd.exe`)*:
```cmd
start "" "C:\Program Files\Google\Chrome\Application\chrome.exe" --remote-debugging-port=9222 --user-data-dir="%TEMP%\rea_audit_profile" https://fifakitcreator.com/
```

#### 2.3 Verifica Diagnostica REA
Per verificare che REA sia correttamente installato e pronto a ricevere chiamate MCP:
```powershell
rea doctor
```

---

### 3. Fasi Operative con i Tool REA MCP

#### Fase 1: Identificazione del Target (`list_browser_targets`)
Individua le schede aperte sul browser per ottenere il `target_id` univoco assegnato a `fifakitcreator.com`:
- **Tool REA**: `list_browser_targets`
- **Parametri**:
  ```json
  {
    "cdp_endpoint": "http://127.0.0.1:9222",
    "allowed_origins": ["https://fifakitcreator.com"]
  }
  ```

#### Fase 2: Ispezione Strutturale e Storage (`inspect_web_page`)
Estrae la mappa dei componenti grafici e lo stato persistente locale:
- **Tool REA**: `inspect_web_page`
- **Parametri**:
  ```json
  {
    "cdp_endpoint": "http://127.0.0.1:9222",
    "target_id": "<TARGET_ID>",
    "allowed_origins": ["https://fifakitcreator.com"],
    "include_accessibility_text": true,
    "include_storage_keys": true,
    "include_storage_fingerprints": true,
    "observation_ms": 3000
  }
  ```
- **Evidenze Rilevate**:
  - Elementi `<canvas>` 3D Three.js.
  - Struttura dei selettori per template maglie, colletti, righe, sponsor (Vue.js scoped CSS `data-v-*`).
  - Storage locale con store `offline` in `CacheStorage` (Evidence ID `ev_2fa77a96...`).
  - Parametri 3D in `LocalStorage`: `["hdr", "lighting2", "createClubPaletteRemappingEnabled", "createClubPreviewColors"]`.

#### Fase 3: Analisi Statica dei Bundle JavaScript (`analyze_web_bundle`)
Effettua il parsing AST statico (senza esecuzione) dei chunk caricati:
- **Tool REA**: `analyze_web_bundle`
- **Moduli Rilevati**:
  - `three.gfa8n7hq.js` (Three.js 3D PBR Engine)
  - `EAFCKitCreator.cscu64c6.js` (Core EA Sports FC Kit Editor)
  - `FIFA21KitCreator.cv845v0b.js` (Modulo di retrocompatibilità)
  - `HotspotsEditor.d4650opc.js` & `Printing.k882p2dk.js` (Posizionamento sponsor, badge e numeri)
  - `freeformPath.fco301ts.js` (Tracciati vettoriali per motivi geometrici)
  - `FifaDownloadModal.di2o1s6x.js` (Modale di confezionamento download)

#### Fase 4: Analisi Disaggregata Rete vs Memoria (Wire vs AST)
- **Misurazione empirica**:
  - **Volume Rete (Wire Size con Brotli)**: 2,17 MiB (2.280.440 byte), dominato al **68,1% dalle texture PNG non comprimibili**.
  - **Footprint Memoria (Heap V8 / RAM)**: 4,31 MiB (4.514.395 byte), dominato al **62,0% dal codice JavaScript decompresso**.

#### Fase 5: Consolidamento del Bundle Evidenze (`export_evidence_bundle`)
Esporta l'intero set di evidenze in un archivio deterministico con checksum per la due diligence:
- **Tool REA**: `export_evidence_bundle`
- **Parametri**:
  ```json
  {
    "path": "C:\\Progetti\\OpenKit Maker\\fifakitcreator_evidence_bundle.rea.zip",
    "overwrite": true
  }
  ```

---

### 4. Matrice Architetturale: Dal Web a Nativo Android (OpenKit Studio)

Il progetto `openkit-studio.zip` contiene **implementazioni concrete già scritte** in Kotlin per la pipeline grafica 2D e 3D:

| Funzionalità Web (fifakitcreator) | Implementazione Web | Implementazione Android Esistente ([`openkit-studio.zip`](file:///c:/Progetti/OpenKit%20Maker/openkit-studio.zip)) | Evoluzione & Priorità #1 |
|---|---|---|---|
| **Viewport 3D Maglia** | Three.js / WebGL Canvas | `Kit3DRenderer.kt` (proiezione 3D software su CPU con illuminazione Gouraud / Blinn-Phong per vertice) | 👉 **Priorità #1 Assoluta (Sblocco Funzionale)**: Migrazione verso **Google Filament / SceneView** su GPU Vulkan/OpenGL ES 3.0 per superare il limite dei < 20 FPS e raggiungere 60–120 FPS stabili |
| **Compositing Texture 2D** | Canvas 2D / WebGL multi-pass | `TextureEngine.kt` (oltre 500 righe di compositing multi-layer, gestione ombre, contorni e rendering numeri) | Ottimizzazione con allocazione diretta `HardwareBuffer` a 2048x2048 |
| **Gestione Decal & Sponsor** | DOM / SVG / Fabric.js | `KitModels.kt` & `KitEditorViewModel.kt` (modelli di dati reattivi con StateFlow) | Supporto coordinate UV scalabili e raycasting interattivo su mesh 3D |
| **Palette Colori & Materiali** | Input HTML color picker | `ColorPickerDialog.kt` (dialog Jetpack Compose Material 3) | Conservazione dello stack Compose esistente |
| **Catalogo Template & Pattern** | XHR Fetch da REST API | `PresetsData.kt` | Integrazione con Room Database (SQLite) per catalogo **100% offline** |
| **Export Texture Finale** | Canvas `.toDataURL('image/png')` | `TextureEngine.kt` (funzione di render bitmap) | Aggiunta pipeline `ZipOutputStream` per formati modding PES / EA FC |

---

### 5. Struttura del Pacchetto di Consegna per il Committente

```
consegna_audit_fifakitcreator/
│
├── 01_Report_Esecutivo_Performance.md      <-- Sintesi prestazionale con tabelle Wire vs Memory e KTX2 vs WebP
├── 02_Mappa_Architetturale_Moduli.md       <-- Dettaglio per team frontend/backend
├── 03_Catalogo_API_ed_Endpoint.json        <-- Specifica formale pattern grafici ed endpoint
├── 04_Piano_Tecnico_Porting_Android.md     <-- Blueprint e roadmap su OpenKit Studio Kotlin
├── run_manifest.json                       <-- Tracciabilità con hash SHA-256 e versioni REA/Chrome
├── notebook_audit_fifakitcreator.ipynb      <-- Quaderno riproducibile eseguito
└── fifakitcreator_evidence_bundle.rea.zip  <-- Evidenze originali REA certificate (1,1 MB)
```
