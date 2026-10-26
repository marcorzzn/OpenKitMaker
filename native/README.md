# OpenKitMaker-Native

> **Target C: Android Nativo (Kotlin 2.0 + Jetpack Compose + Google Filament PBR)**  
> **Autore:** [Marco Razzano](https://github.com/marcorzzn) (`@marcorzzn`)  
> **Licenza:** [MIT](../LICENSE)

Applicazione Android nativa per la progettazione, composizione 2D (2048×2048 RGBA) e visualizzazione 3D PBR a 60 FPS di kit grafici sportivi, sviluppata secondo il Design System ufficiale di **OpenKitMaker**.

---

## Architettura e Stack Tecnologico

- **Linguaggio & SDK**: Kotlin 2.0.21, Min SDK 26 (Android 8.0), Compile SDK 35 (Android 15), Target SDK 34 (Android 14)
- **Application ID**: `com.marcorazzano.openkitmaker`
- **UI & Navigazione**: Jetpack Compose (Material 3), Single-Activity (`MainActivity`), Navigation Compose
- **Architettura**: MVVM + Clean Architecture (`domain`, `data`, `render`, `export`, `ui`, `di`, `i18n`) con `StateFlow`, Coroutines e **Hilt**
- **Rendering 3D GPU**: **Google Filament / SceneView** (`io.github.sceneview:sceneview:2.2.1`) su backend Vulkan / OpenGL ES 3.0 (60 FPS stabili, zero allocazioni JNI nel loop di render).
- **Compositing 2D**: `TextureEngine.kt` su `Bitmap` / `Canvas` hardware-accelerated 2048×2048 con supporto per testo curvato su arco (`drawTextOnPath`), numeri bicolore con contorno, pattern geometrici procedurali e blend mode (`NORMAL`, `MULTIPLY`, `OVERLAY`).
- **Persistenza Offline**: **Room** (SQLite) con salvataggio locale e cache minikit 512×512.
- **Esportazione Asincrona**: **WorkManager** (`CoroutineWorker`) per l'esportazione lossless di `PNG 2048×2048` e pacchetti interoperabili `kit_<id>.zip` (`texture.png`, `preview.png`, `manifest.json` con checksum SHA-256) su `MediaStore.Downloads` (Scoped Storage Android 10+).

---

## Tracciabilità Specifiche Design System (`SCR-*` / `FUN-*`)

| ID Specifiche | Componente / File | Descrizione |
| :--- | :--- | :--- |
| `SCR-HOME` | `ui/home/HomeScreen.kt`, `HomeViewModel.kt` | Dashboard con stato rete offline/online, ultime 3 bozze da Room, CTA nuovo kit |
| `SCR-CATALOG` | `ui/catalog/CatalogScreen.kt` | Griglia template da Room, filtri a chip (`HOME`, `AWAY`, `THIRD`, `GK`, `SPECIAL`), ricerca |
| `SCR-EDITOR` | `ui/editor/EditorScreen.kt`, `EditorViewModel.kt` | Editor a livelli, selettore vista (`2D`, `3D`, `SPLIT`), Color Picker HSV, toggle luci Studio/Stadio |
| `SCR-SAVED` | `ui/saved/SavedKitsScreen.kt` | Lista kit Room, rinomina, duplica, importa ZIP, elimina con `AlertDialog` di conferma |
| `SCR-EXPORT` | `ui/export/ExportScreen.kt`, `ExportViewModel.kt` | Anteprima finale 2048×2048 e 512×512, export PNG lossless e ZIP interoperabile via WorkManager |
| `FUN-SELECT-TEMPLATE` | `data/repository/KitRepositoryImpl.kt` | Clonazione profonda dei layer del template con nuovo UUIDv4 |
| `FUN-MANAGE-LAYERS` | `ui/components/LayerList.kt`, `EditorViewModel.kt` | CRUD layer, z-index, opacità `0.0..1.0`, visibilità, blend mode |
| `FUN-EDIT-COLOR` | `ui/components/ColorPicker.kt` | Selettore HSV, input `#RRGGBB`/`#RRGGBBAA` e storico ultimi 8 colori |
| `FUN-COMPOSITE-2D` | `render/TextureEngine.kt` | Compositing 2048×2048 RGBA con nome curvato, numero bicolore e pattern geometrici |
| `FUN-PREVIEW-3D` | `render/Kit3DRenderer.kt`, `MaterialFactory.kt` | Rendering PBR 60 FPS con Google Filament/SceneView, camera orbitale con pitch vincolato |
| `FUN-SAVE-LOCAL` | `data/local/KitDatabase.kt`, `KitDao.kt` | Persistenza SQLite Room e generazione minikit 512×512 |
| `FUN-EXPORT-PNG` | `export/PngExporter.kt` | Esportazione `texture.png` 2048×2048 su `MediaStore` via WorkManager |
| `FUN-EXPORT-ZIP` | `export/ZipExporter.kt` | Creazione `kit_<id>.zip` (`texture.png`, `preview.png`, `manifest.json` con SHA-256) |
| `FUN-OFFLINE-MODE` | Tutte le schermate | Funzionamento 100% autonomo senza connessione di rete |

---

## Istruzioni di Compilazione (Gradle 8.9)

```bash
# Compilazione APK Debug
./gradlew :app:assembleDebug

# Compilazione Bundle Release (AAB) per Google Play Store
./gradlew :app:bundleRelease
```

---

## Autore

**Marco Razzano** — [@marcorzzn](https://github.com/marcorzzn)
