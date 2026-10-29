// Implements: SCR-HOME / SCR-CATALOG / SCR-EDITOR / SCR-SAVED / SCR-EXPORT / FUN-SELECT-TEMPLATE / FUN-MANAGE-LAYERS / FUN-EDIT-COLOR / FUN-EXPORT-ZIP
package com.openkitmaker.android.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
enum class KitCategory(val labelIt: String) {
    HOME("Casa"),
    AWAY("Trasferta"),
    THIRD("Terza"),
    GK("Portiere"),
    SPECIAL("Speciali")
}

@Serializable
enum class LayerType(val labelIt: String) {
    BASE("Base"),
    PATTERN("Strisce & Pattern"),
    COLLAR("Colletto"),
    SLEEVES("Maniche"),
    SPONSOR("Sponsor Astratto"),
    BADGE("Stemma Geometrico"),
    TEXT_NAME("Nome Curvato"),
    NUMBER("Numero Bicolore")
}

@Serializable
enum class BlendMode(val labelIt: String) {
    NORMAL("Normale"),
    MULTIPLY("Moltiplica"),
    OVERLAY("Sovrapponi"),
    SCREEN("Scherma")
}

@Serializable
enum class PatternStyle(val labelIt: String) {
    SOLID("Tinta Unita"),
    VERTICAL_STRIPES("Strisce Verticali"),
    HORIZONTAL_HOOPS("Bande Orizzontali"),
    DIAGONAL_SASH("Fascia Diagonale"),
    CHEVRON("Chevron a V"),
    CHECKERBOARD("Scacchiera"),
    HALVES("Bicolore Partito"),
    PINSTRIPES("Gessato Sottile"),
    HEX_GRID("Griglia Esagonale"),
    DIAMOND_ARGYLE("Rombi Geometrici"),
    HEXAGONS("Esagoni Procedurali"),
    WAVES("Onde Sinusoidali"),
    CAMOUFLAGE("Mimetico Organico"),
    ABSTRACT_GEOMETRIC("Geometria Astratta")
}

@Serializable
enum class CollarStyle(val labelIt: String) {
    CREW_NECK("Girocollo Classico"),
    V_NECK("Scollo a V"),
    POLO_RIB("Colletto Polo"),
    STAND_COLLAR("Colletto Coreano")
}

@Serializable
enum class DecalShape(val labelIt: String) {
    HEXAGON_SHIELD("Scudo Esagonale"),
    CONCENTRIC_ROUNDEL("Coccarda Concentrica"),
    DIAMOND_CREST("Losanga Atletica"),
    VECTOR_WINGS("Ali Geometriche"),
    TRIPLE_BARS("Barre Parallele"),
    STAR_EMBLEM("Stella Poligonale")
}

@Serializable
enum class LightingMode(
    val labelIt: String,
    val intensityMultiplier: Float,
    val kelvinTemp: Int
) {
    STUDIO("Studio (6500K)", 1.2f, 6500),
    STADIUM("Stadio (5400K)", 1.8f, 5400),
    SUNSET("Tramonto (3200K)", 1.5f, 3200),
    RADIANT("Luce Radiante (8500K)", 2.0f, 8500)
}

@Serializable
enum class ViewportBackground(
    val labelIt: String,
    val hexColor: Long,
    val emoji: String,
    val isPitch: Boolean = false
) {
    DARK_SOLID("Scuro Solido", 0xFF0F141CL, "⬛"),
    STUDIO_NEUTRAL("Studio Neutro", 0xFF1E2530L, "🏢"),
    PITCH_FIELD("Campo da Calcio", 0xFF0B3018L, "⚽", true)
}

@Serializable
enum class CameraPreset(val labelIt: String) {
    FRONT("Fronte"),
    BACK("Retro"),
    LEFT("Lato SX"),
    RIGHT("Lato DX")
}

@Serializable
enum class FabricType(
    val labelIt: String,
    val description: String,
    val emoji: String,
    val defaultRoughness: Float,
    val defaultMetalness: Float
) {
    MESH("Mesh Traspirante", "Tessuto tecnico con micro-fori traspiranti, finitura opaca", "🎽", 0.78f, 0.02f),
    POLYESTER("Poliestere Lucido", "Tessuto sintetico riflettente con riflesso plastificato", "✨", 0.22f, 0.12f),
    COTTON("Cotone Anni '90", "Trama pesante e ruvida ad altissima opacità", "👕", 0.90f, 0.01f),
    JACQUARD("Jacquard", "Tessuto nobile con trama in rilievo geometrico tridimensionale", "🏛️", 0.48f, 0.06f)
}

@Serializable
enum class ExportResolution(val pixelSize: Int, val label: String) {
    RES_2K(2048, "2K (2048x2048)"),
    RES_4K(4096, "4K Ultra HD (4096x4096)")
}

@Serializable
enum class ExportFormatType(val labelIt: String, val extension: String) {
    PNG_2K("Texture PNG 2K", "png"),
    PNG_4K("Texture PNG 4K Ultra HD", "png"),
    ZIP_FULL_KIT("Archivio Full Kit (.ZIP)", "zip"),
    PBR_BUNDLE("Bundle Materiali PBR (.ZIP)", "zip"),
    GLB_3D("Modello 3D (.GLB)", "glb")
}

@Serializable
enum class EditorViewMode(val labelIt: String) {
    VIEW_2D("Vista 2D"),
    VIEW_3D("Vista 3D"),
    VIEW_SPLIT("Vista Doppia")
}

@Serializable
data class Decal(
    val id: String = UUID.randomUUID().toString(),
    val shape: DecalShape = DecalShape.HEXAGON_SHIELD,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val primaryColor: String = "#FFFFFF",
    val secondaryColor: String = "#00C49F"
)

@Serializable
data class LayerProperties(
    val patternStyle: PatternStyle = PatternStyle.VERTICAL_STRIPES,
    val patternFrequency: Int = 7,
    val collarStyle: CollarStyle = CollarStyle.V_NECK,
    val textValue: String = "",
    val textArcDegrees: Float = 24f,
    val fontSize: Float = 64f,
    val strokeWidth: Float = 6f,
    val frontNumberVisible: Boolean = true,
    val decal: Decal? = null,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val patternScale: Float = 1f,
    val patternRotation: Float = 0f,
    val snappedToGuide: Boolean = false
)

@Serializable
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: LayerType,
    val visible: Boolean = true,
    val opacity: Float = 1.0f,
    val zIndex: Int = 0,
    val color: String = "#0F52BA",
    val secondaryColor: String? = "#FFFFFF",
    val blendMode: BlendMode = BlendMode.NORMAL,
    val properties: LayerProperties = LayerProperties()
)

@Serializable
data class KitMetadata(
    val author: String = "OpenKitMaker-Native",
    val roughness: Float = 0.48f,
    val metalness: Float = 0.06f,
    val fabricType: FabricType = FabricType.MESH,
    val lightingMode: LightingMode = LightingMode.STUDIO,
    val viewportBackground: ViewportBackground = ViewportBackground.DARK_SOLID,
    val showAnatomicalView: Boolean = false,
    val recentColors: List<String> = listOf(
        "#0F52BA", "#00C49F", "#FFFFFF", "#0F141C",
        "#EF4444", "#F59E0B", "#10B981", "#6366F1"
    )
)

@Serializable
enum class GarmentType(val labelIt: String, val emoji: String) {
    JERSEY("Maglia", "👕"),
    SHORTS("Pantaloncini", "🩳"),
    SOCKS("Calzettoni", "🧦")
}

@Serializable
data class Garment(
    val type: GarmentType,
    val layers: List<Layer>,
    val baseColor: String = "#0F52BA",
    val secondaryColor: String? = "#00C49F"
)

@Serializable
data class KitGarments(
    val jersey: Garment,
    val shorts: Garment,
    val socks: Garment
)

fun createDefaultGarments(jerseyLayers: List<Layer>): KitGarments {
    val baseColor = jerseyLayers.firstOrNull { it.type == LayerType.BASE }?.color ?: "#0F52BA"
    val secColor = jerseyLayers.firstOrNull { it.type == LayerType.BASE }?.secondaryColor ?: "#00C49F"

    val shortsLayers = listOf(
        Layer(
            id = UUID.randomUUID().toString(),
            name = "Base Pantaloncino",
            type = LayerType.BASE,
            color = "#09111E",
            secondaryColor = baseColor,
            zIndex = 0
        ),
        Layer(
            id = UUID.randomUUID().toString(),
            name = "Banda Laterale",
            type = LayerType.PATTERN,
            color = secColor,
            zIndex = 1,
            properties = LayerProperties(
                patternStyle = PatternStyle.VERTICAL_STRIPES,
                patternFrequency = 1
            )
        ),
        Layer(
            id = UUID.randomUUID().toString(),
            name = "Stemma Coscia",
            type = LayerType.BADGE,
            color = secColor,
            secondaryColor = "#FFFFFF",
            zIndex = 2
        ),
        Layer(
            id = UUID.randomUUID().toString(),
            name = "Numero Coscia",
            type = LayerType.NUMBER,
            color = "#FFFFFF",
            zIndex = 3,
            properties = LayerProperties(
                textValue = "9",
                fontSize = 48f
            )
        )
    )

    val socksLayers = listOf(
        Layer(
            id = UUID.randomUUID().toString(),
            name = "Base Calzettone",
            type = LayerType.BASE,
            color = baseColor,
            secondaryColor = "#FFFFFF",
            zIndex = 0
        ),
        Layer(
            id = UUID.randomUUID().toString(),
            name = "Bande Polpaccio",
            type = LayerType.PATTERN,
            color = "#FFFFFF",
            zIndex = 1,
            properties = LayerProperties(
                patternStyle = PatternStyle.HORIZONTAL_HOOPS,
                patternFrequency = 3
            )
        ),
        Layer(
            id = UUID.randomUUID().toString(),
            name = "Risvolto",
            type = LayerType.COLLAR,
            color = secColor,
            zIndex = 2
        )
    )

    return KitGarments(
        jersey = Garment(type = GarmentType.JERSEY, layers = jerseyLayers, baseColor = baseColor, secondaryColor = secColor),
        shorts = Garment(type = GarmentType.SHORTS, layers = shortsLayers, baseColor = "#09111E", secondaryColor = secColor),
        socks = Garment(type = GarmentType.SOCKS, layers = socksLayers, baseColor = baseColor, secondaryColor = "#FFFFFF")
    )
}

@Serializable
data class Kit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: KitCategory = KitCategory.HOME,
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String = Instant.now().toString(),
    val templateId: String,
    val layers: List<Layer>,
    val garments: KitGarments = createDefaultGarments(layers),
    val metadata: KitMetadata = KitMetadata()
)

@Serializable
data class Template(
    val id: String,
    val name: String,
    val category: KitCategory,
    val description: String,
    val defaultLayers: List<Layer>
)

@Serializable
data class GeneratorInfo(
    val platform: String = "OpenKitMaker-Native",
    val version: String = "1.1.0",
    val buildNumber: String = "110"
)

@Serializable
data class ExportDimensions(
    val width: Int = 2048,
    val height: Int = 2048
)

@Serializable
data class ExportChecksums(
    val textureSha256: String,
    val previewSha256: String,
    val jerseySha256: String? = null,
    val shortsSha256: String? = null,
    val socksSha256: String? = null
)

@Serializable
data class ManifestKitData(
    val templateId: String,
    val layers: List<Layer>,
    val garments: KitGarments? = null,
    val metadata: KitMetadata? = null
)

@Serializable
data class ExportManifest(
    @SerialName("\$schema")
    val schema: String = "https://openkitmaker.dev/schemas/kit-manifest-v1.json",
    val version: String = "1.0.0",
    val kitId: String,
    val name: String,
    val category: KitCategory,
    val exportTimestamp: String,
    val generator: GeneratorInfo = GeneratorInfo(),
    val dimensions: ExportDimensions = ExportDimensions(),
    val checksums: ExportChecksums,
    val kitData: ManifestKitData
)
