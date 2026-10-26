# OpenKitMaker — Modello Dati e Strutture di Persistenza (Specifica di Parità)

Documento di specifica per le entità di dominio di **OpenKitMaker**.
I modelli devono essere implementati con gli stessi nomi di campi e tipi sia in TypeScript (`web/src/lib/domain/`) che in Kotlin (`native/app/src/main/java/com/openkitmaker/native/domain/`).

---

## 1. Entità Principali

### `Kit`
Rappresenta l'intero progetto grafico della divisa.

```typescript
interface Kit {
  id: string;                    // UUID v4 (es. "a2c5b364-7f12-401d-91b6-681b4df12a9e")
  name: string;                  // Nome del kit (es. "Milano Home 2026")
  category: KitCategory;         // "HOME" | "AWAY" | "THIRD" | "GK" | "SPECIAL"
  createdAt: string;             // ISO-8601 UTC
  updatedAt: string;             // ISO-8601 UTC
  templateId: string;            // ID del template di origine
  layers: Layer[];               // Elenco ordinato dei layer (dal basso verso l'alto)
  metadata: KitMetadata;         // Metadati descrittivi e di generazione
}

type KitCategory = "HOME" | "AWAY" | "THIRD" | "GK" | "SPECIAL";

interface KitMetadata {
  author?: string;
  version: string;               // Schema version, es. "1.0.0"
  description?: string;
  generatorPlatform: "WEB" | "NATIVE";
}
```

### `Layer`
Rappresenta un singolo livello grafico applicato alla texture.

```typescript
interface Layer {
  id: string;                    // UUID univoco del livello
  name: string;                  // Nome visualizzato (es. "Strisce Verticali")
  type: LayerType;               // Tipologia del componente
  visible: boolean;              // Visibilità attiva/inattiva
  opacity: number;               // 0.0 (trasparente) .. 1.0 (opaco)
  zIndex: number;                // Posizione nello stack di rendering (0, 1, 2...)
  color: string;                 // Colore primario formato HEX "#RRGGBB" o "#RRGGBBAA"
  secondaryColor?: string;       // Colore secondario opzionale
  blendMode: BlendMode;          // "NORMAL" | "MULTIPLY" | "OVERLAY"
  properties: Record<string, any>; // Proprietà parametriche specifiche del pattern
}

type LayerType = 
  | "BASE"                       // Texture tinta unita di base
  | "PATTERN"                    // Pattern geometrico (strisce, scacchi, rombi)
  | "COLLAR"                     // Finitura e dettagli del colletto
  | "SLEEVES"                    // Inserti maniche / bordi
  | "SPONSOR"                    // Logo sponsor frontale
  | "BADGE"                      // Stemma societario / scudetto
  | "TEXT_NAME"                  // Nome giocatore su dorso
  | "NUMBER";                    // Numero di maglia

type BlendMode = "NORMAL" | "MULTIPLY" | "OVERLAY";
```

### `Decal` (Elemento Vettoriale / Grafico Posizionato)
Rappresenta loghi, testi, numeri posizionati in coordinate UV.

```typescript
interface Decal {
  id: string;
  layerId: string;
  assetKey: string;              // Riferimento all'asset (es. "badge_circle", "sponsor_standard")
  targetRegion: UVRegion;        // "CHEST_LEFT" | "CHEST_CENTER" | "BACK_NAME" | "BACK_NUMBER"
  posX: number;                  // Coordinata normalizzata X (0.0 .. 1.0)
  posY: number;                  // Coordinata normalizzata Y (0.0 .. 1.0)
  scaleX: number;                // Fattore scala X
  scaleY: number;                // Fattore scala Y
  rotationDeg: number;           // Rotazione in gradi (-180 .. 180)
  color: string;                 // Tinta applicata all'elemento
}

type UVRegion = "CHEST_LEFT" | "CHEST_RIGHT" | "CHEST_CENTER" | "BACK_NAME" | "BACK_NUMBER" | "SLEEVE_LEFT" | "SLEEVE_RIGHT";
```

### `Template`
Definizione statica di un modello da catalogo.

```typescript
interface Template {
  id: string;                    // es. "tpl_classic_stripes_01"
  name: string;                  // es. "Righe Classiche"
  category: KitCategory;
  thumbnailUrl: string;          // Percorso thumbnail preview 512x512
  meshModel: string;             // File modello 3D (es. "models/jersey_base.glb")
  defaultLayers: Layer[];        // Lista layer iniziali preconfigurati
}
```

---

## 2. Mappatura Kotlin Corrispondente (Android Room & Domain)

In Kotlin, la struttura viene mappata fedelmente in `data class`:

```kotlin
// native/app/src/main/java/com/openkitmaker/native/domain/model/Kit.kt

data class Kit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: KitCategory = KitCategory.HOME,
    val createdAt: String,
    val updatedAt: String,
    val templateId: String,
    val layers: List<Layer>,
    val metadata: KitMetadata
)

enum class KitCategory { HOME, AWAY, THIRD, GK, SPECIAL }

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: LayerType,
    val visible: Boolean = true,
    val opacity: Float = 1.0f,
    val zIndex: Int,
    val color: String,
    val secondaryColor: String? = null,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val properties: Map<String, String> = emptyMap()
)

enum class LayerType {
    BASE, PATTERN, COLLAR, SLEEVES, SPONSOR, BADGE, TEXT_NAME, NUMBER
}

enum class BlendMode { NORMAL, MULTIPLY, OVERLAY }
```

---

## 3. Regole di Serializzazione e Compatibilità

1. I colori sono sempre memorizzati come stringhe HEX con cancelletto iniziale: `"#RRGGBB"` o `"#RRGGBBAA"`. Nessun formato intero (es. `0xFF112233`) o stringa CSS `rgb()` deve essere usato nella persistenza.
2. I timestamp sono sempre in formato ISO-8601 UTC terminanti con `Z` (es. `"2026-10-26T18:15:00Z"`).
3. L'esportazione su JSON deve sempre utilizzare chiavi in formato `camelCase`.
