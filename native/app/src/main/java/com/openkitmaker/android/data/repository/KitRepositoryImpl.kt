// Implements: SCR-HOME / SCR-CATALOG / SCR-SAVED / FUN-SELECT-TEMPLATE / FUN-SAVE-LOCAL / FUN-OFFLINE-MODE
package com.openkitmaker.android.data.repository

import com.openkitmaker.android.data.local.KitDao
import com.openkitmaker.android.data.local.KitEntity
import com.openkitmaker.android.data.local.TemplateEntity
import com.openkitmaker.android.domain.BlendMode
import com.openkitmaker.android.domain.CollarStyle
import com.openkitmaker.android.domain.Decal
import com.openkitmaker.android.domain.DecalShape
import com.openkitmaker.android.domain.Kit
import com.openkitmaker.android.domain.KitCategory
import com.openkitmaker.android.domain.KitMetadata
import com.openkitmaker.android.domain.Layer
import com.openkitmaker.android.domain.LayerProperties
import com.openkitmaker.android.domain.LayerType
import com.openkitmaker.android.domain.PatternStyle
import com.openkitmaker.android.domain.Template
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface KitRepository {
    fun observeAllKits(): Flow<List<Kit>>
    fun observeRecentKits(): Flow<List<Kit>>
    fun observeTemplates(): Flow<List<Template>>
    suspend fun getKitById(id: String): Kit?
    suspend fun cloneKitFromTemplate(template: Template): Kit
    suspend fun saveKit(kit: Kit)
    suspend fun renameKit(kitId: String, newName: String)
    suspend fun duplicateKit(kitId: String): Kit?
    suspend fun deleteKit(kitId: String)
    suspend fun ensureSeeded()
}

@Singleton
class KitRepositoryImpl @Inject constructor(
    private val kitDao: KitDao,
    private val json: Json
) : KitRepository {

    override fun observeAllKits(): Flow<List<Kit>> =
        kitDao.observeAllKits().map { list -> list.map { it.toDomain() } }

    override fun observeRecentKits(): Flow<List<Kit>> =
        kitDao.observeRecentKits().map { list -> list.map { it.toDomain() } }

    override fun observeTemplates(): Flow<List<Template>> =
        kitDao.observeTemplates().map { list -> list.map { it.toDomain() } }

    override suspend fun getKitById(id: String): Kit? =
        kitDao.getKitById(id)?.toDomain()

    /**
     * FUN-SELECT-TEMPLATE: Clones template layers with brand-new UUIDv4 identifiers.
     */
    override suspend fun cloneKitFromTemplate(template: Template): Kit {
        val now = Instant.now().toString()
        val clonedLayers = template.defaultLayers.mapIndexed { index, layer ->
            layer.copy(
                id = UUID.randomUUID().toString(),
                zIndex = index
            )
        }
        val newKit = Kit(
            id = UUID.randomUUID().toString(),
            name = "${template.name} Bozza",
            category = template.category,
            createdAt = now,
            updatedAt = now,
            templateId = template.id,
            layers = clonedLayers
        )
        saveKit(newKit)
        return newKit
    }

    override suspend fun saveKit(kit: Kit) {
        val updated = kit.copy(updatedAt = Instant.now().toString())
        kitDao.upsertKit(updated.toEntity())
    }

    override suspend fun renameKit(kitId: String, newName: String) {
        kitDao.renameKit(
            kitId = kitId,
            newName = newName.trim().ifEmpty { "Kit Senza Nome" },
            updatedAt = Instant.now().toString()
        )
    }

    override suspend fun duplicateKit(kitId: String): Kit? {
        val original = getKitById(kitId) ?: return null
        val now = Instant.now().toString()
        val copyKit = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copia)",
            createdAt = now,
            updatedAt = now,
            layers = original.layers.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        saveKit(copyKit)
        return copyKit
    }

    override suspend fun deleteKit(kitId: String) {
        kitDao.deleteKitById(kitId)
    }

    override suspend fun ensureSeeded() {
        if (kitDao.countTemplates() > 0) return
        val defaults = buildBuiltInTemplates()
        kitDao.insertTemplates(defaults.map { it.toEntity() })
        // Seed initial sample draft kits for offline SCR-HOME readiness
        defaults.take(3).forEach { tpl ->
            cloneKitFromTemplate(tpl)
        }
    }

    private fun KitEntity.toDomain(): Kit = Kit(
        id = id,
        name = name,
        category = runCatching { KitCategory.valueOf(category) }.getOrDefault(KitCategory.HOME),
        createdAt = createdAt,
        updatedAt = updatedAt,
        templateId = templateId,
        layers = json.decodeFromString(layersJson),
        metadata = runCatching { json.decodeFromString<KitMetadata>(metadataJson) }
            .getOrDefault(KitMetadata())
    )

    private fun Kit.toEntity(): KitEntity = KitEntity(
        id = id,
        name = name,
        category = category.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        templateId = templateId,
        layersJson = json.encodeToString(layers),
        metadataJson = json.encodeToString(metadata)
    )

    private fun TemplateEntity.toDomain(): Template = Template(
        id = id,
        name = name,
        category = runCatching { KitCategory.valueOf(category) }.getOrDefault(KitCategory.HOME),
        description = description,
        defaultLayers = json.decodeFromString(defaultLayersJson)
    )

    private fun Template.toEntity(): TemplateEntity = TemplateEntity(
        id = id,
        name = name,
        category = category.name,
        description = description,
        defaultLayersJson = json.encodeToString(defaultLayers)
    )

    companion object {
        fun buildBuiltInTemplates(): List<Template> = listOf(
            Template(
                id = "tpl_home_vertical",
                name = "Classico Verticale Casa",
                category = KitCategory.HOME,
                description = "Palatura verticale bicolore con colletto a V e bordi a contrasto.",
                defaultLayers = buildStandardLayers(
                    baseColor = "#0F52BA",
                    patternStyle = PatternStyle.VERTICAL_STRIPES,
                    patternColor = "#091E42",
                    collarStyle = CollarStyle.V_NECK,
                    accentColor = "#00C49F",
                    sponsorText = "AERO DYNAMICS",
                    playerName = "ROSSI",
                    playerNumber = "10"
                )
            ),
            Template(
                id = "tpl_away_sash",
                name = "Diagonale Trasferta",
                category = KitCategory.AWAY,
                description = "Fondo chiaro con fascia diagonale geometrica e colletto polo.",
                defaultLayers = buildStandardLayers(
                    baseColor = "#F8FAFC",
                    patternStyle = PatternStyle.DIAGONAL_SASH,
                    patternColor = "#0F52BA",
                    collarStyle = CollarStyle.POLO_RIB,
                    accentColor = "#0A387E",
                    sponsorText = "NOVA LABS",
                    playerName = "BIANCHI",
                    playerNumber = "7"
                )
            ),
            Template(
                id = "tpl_third_chevron",
                name = "Terza Maglia Chevron",
                category = KitCategory.THIRD,
                description = "Geometria a V multipla su base antracite con dettagli smeraldo.",
                defaultLayers = buildStandardLayers(
                    baseColor = "#0F141C",
                    patternStyle = PatternStyle.CHEVRON,
                    patternColor = "#00C49F",
                    collarStyle = CollarStyle.CREW_NECK,
                    accentColor = "#10B981",
                    sponsorText = "SOLAR GRID",
                    playerName = "CONTI",
                    playerNumber = "21"
                )
            ),
            Template(
                id = "tpl_gk_hex",
                name = "Portiere Esagonale Pro",
                category = KitCategory.GK,
                description = "Trama a nido d'ape ad alto contrasto per estremo difensore.",
                defaultLayers = buildStandardLayers(
                    baseColor = "#10B981",
                    patternStyle = PatternStyle.HEX_GRID,
                    patternColor = "#064E3B",
                    collarStyle = CollarStyle.STAND_COLLAR,
                    accentColor = "#FFFFFF",
                    sponsorText = "APEX GUARD",
                    playerName = "FERRARI",
                    playerNumber = "1"
                )
            ),
            Template(
                id = "tpl_special_argyle",
                name = "Edizione Speciale Rombi",
                category = KitCategory.SPECIAL,
                description = "Pattern geometrico a losanghe commemorative con finiture dorate.",
                defaultLayers = buildStandardLayers(
                    baseColor = "#311042",
                    patternStyle = PatternStyle.DIAMOND_ARGYLE,
                    patternColor = "#F59E0B",
                    collarStyle = CollarStyle.POLO_RIB,
                    accentColor = "#F59E0B",
                    sponsorText = "VECTOR STUDIO",
                    playerName = "CAPITANO",
                    playerNumber = "9"
                )
            )
        )

        private fun buildStandardLayers(
            baseColor: String,
            patternStyle: PatternStyle,
            patternColor: String,
            collarStyle: CollarStyle,
            accentColor: String,
            sponsorText: String,
            playerName: String,
            playerNumber: String
        ): List<Layer> = listOf(
            Layer(
                name = "Base Maglia",
                type = LayerType.BASE,
                zIndex = 0,
                color = baseColor,
                secondaryColor = patternColor
            ),
            Layer(
                name = "Trama Geometrica",
                type = LayerType.PATTERN,
                zIndex = 1,
                color = patternColor,
                secondaryColor = baseColor,
                properties = LayerProperties(patternStyle = patternStyle, patternFrequency = 7)
            ),
            Layer(
                name = "Maniche e Bordi",
                type = LayerType.SLEEVES,
                zIndex = 2,
                color = baseColor,
                secondaryColor = accentColor
            ),
            Layer(
                name = "Colletto",
                type = LayerType.COLLAR,
                zIndex = 3,
                color = accentColor,
                secondaryColor = baseColor,
                properties = LayerProperties(collarStyle = collarStyle)
            ),
            Layer(
                name = "Stemma Geometrico",
                type = LayerType.BADGE,
                zIndex = 4,
                color = accentColor,
                secondaryColor = "#FFFFFF",
                properties = LayerProperties(
                    decal = Decal(
                        shape = DecalShape.HEXAGON_SHIELD,
                        posX = -0.22f,
                        posY = -0.20f,
                        scale = 1.0f,
                        primaryColor = accentColor,
                        secondaryColor = "#FFFFFF"
                    )
                )
            ),
            Layer(
                name = "Sponsor Frontale",
                type = LayerType.SPONSOR,
                zIndex = 5,
                color = if (baseColor == "#F8FAFC") "#0F141C" else "#FFFFFF",
                secondaryColor = accentColor,
                properties = LayerProperties(textValue = sponsorText, fontSize = 56f)
            ),
            Layer(
                name = "Nome Giocatore Curvato",
                type = LayerType.TEXT_NAME,
                zIndex = 6,
                color = if (baseColor == "#F8FAFC") "#0F141C" else "#FFFFFF",
                secondaryColor = "#0F141C",
                properties = LayerProperties(
                    textValue = playerName,
                    textArcDegrees = 26f,
                    fontSize = 68f
                )
            ),
            Layer(
                name = "Numero Bicolore",
                type = LayerType.NUMBER,
                zIndex = 7,
                color = if (baseColor == "#F8FAFC") "#0F52BA" else "#FFFFFF",
                secondaryColor = accentColor,
                properties = LayerProperties(
                    textValue = playerNumber,
                    fontSize = 240f,
                    strokeWidth = 8f,
                    frontNumberVisible = true
                )
            )
        )
    }
}
