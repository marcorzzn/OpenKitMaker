// Implements: SCR-CATALOG / FUN-SELECT-TEMPLATE
package com.openkitmaker.android.data

import com.openkitmaker.android.data.repository.KitRepositoryImpl
import com.openkitmaker.android.domain.CollarStyle
import com.openkitmaker.android.domain.KitCategory
import com.openkitmaker.android.domain.PatternStyle
import com.openkitmaker.android.domain.Template

/**
 * Catalogo preset Full Kit (Maglia + Pantaloncini + Calzettoni) per Android.
 * Contiene i 5 nuovi template preimpostati conformi alla parità Web:
 * - Banda Classica
 * - Maniche a Contrasto
 * - Design Minimal
 * - Scaglione Atletico
 * - Geometric Vapor
 */
object PresetsData {

    fun getFullKitTemplates(): List<Template> = listOf(
        // 1. Banda Classica
        Template(
            id = "tpl-fullkit-banda-classica",
            name = "Banda Classica",
            category = KitCategory.HOME,
            description = "Completo tradizionale con maglia rosso corsa a fascia orizzontale, pantaloncini blu navy e calzettoni con righe bianche.",
            defaultLayers = KitRepositoryImpl.buildStandardLayers(
                baseColor = "#DC2626",
                patternStyle = PatternStyle.HORIZONTAL_HOOPS,
                patternColor = "#FFFFFF",
                collarStyle = CollarStyle.V_NECK,
                accentColor = "#0F172A",
                sponsorText = "CLASSIC",
                playerName = "MAZZOLA",
                playerNumber = "9"
            )
        ),
        // 2. Maniche a Contrasto
        Template(
            id = "tpl-fullkit-maniche-contrasto",
            name = "Maniche a Contrasto",
            category = KitCategory.AWAY,
            description = "Divisa solare giallo canarino con maniche raglan nere ad alto impatto visivo, shorts e calzettoni neri.",
            defaultLayers = KitRepositoryImpl.buildStandardLayers(
                baseColor = "#EAB308",
                patternStyle = PatternStyle.HALVES,
                patternColor = "#0F141C",
                collarStyle = CollarStyle.CREW_NECK,
                accentColor = "#0F141C",
                sponsorText = "VELOX",
                playerName = "PIRLO",
                playerNumber = "21"
            )
        ),
        // 3. Design Minimal
        Template(
            id = "tpl-fullkit-design-minimal",
            name = "Design Minimal",
            category = KitCategory.THIRD,
            description = "Kit sobrio ed elegante in grafite titanio con sottili gessature scure, finiture argento e shorts coordinati.",
            defaultLayers = KitRepositoryImpl.buildStandardLayers(
                baseColor = "#334155",
                patternStyle = PatternStyle.PINSTRIPES,
                patternColor = "#1E293B",
                collarStyle = CollarStyle.STAND_COLLAR,
                accentColor = "#E2E8F0",
                sponsorText = "STEALTH",
                playerName = "NESTA",
                playerNumber = "13"
            )
        ),
        // 4. Scaglione Atletico
        Template(
            id = "tpl-fullkit-scaglione-atletico",
            name = "Scaglione Atletico",
            category = KitCategory.SPECIAL,
            description = "Azzurro elettrico attraversato da chevron dinamico su petto, pantaloncini candidi con bande azzurre e calzettoni coordinate.",
            defaultLayers = KitRepositoryImpl.buildStandardLayers(
                baseColor = "#0284C7",
                patternStyle = PatternStyle.CHEVRON,
                patternColor = "#FFFFFF",
                collarStyle = CollarStyle.V_NECK,
                accentColor = "#0C4A6E",
                sponsorText = "ATLETICA",
                playerName = "ZOLA",
                playerNumber = "10"
            )
        ),
        // 5. Geometric Vapor
        Template(
            id = "tpl-fullkit-geometric-vapor",
            name = "Geometric Vapor",
            category = KitCategory.GK,
            description = "Divisa ad alto contrasto fluo con raggi geometrici su base viola ametista, pantaloncini scuri e calze coordinate.",
            defaultLayers = KitRepositoryImpl.buildStandardLayers(
                baseColor = "#7C3AED",
                patternStyle = PatternStyle.HEX_GRID,
                patternColor = "#06B6D4",
                collarStyle = CollarStyle.STAND_COLLAR,
                accentColor = "#06B6D4",
                sponsorText = "VAPOR·X",
                playerName = "BUFFON",
                playerNumber = "1"
            )
        )
    )
}
