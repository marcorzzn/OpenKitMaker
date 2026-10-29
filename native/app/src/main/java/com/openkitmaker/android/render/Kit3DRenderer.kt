// Implements: SCR-EDITOR / FUN-PREVIEW-3D / FUN-EDIT-SHORTS / FUN-EDIT-SOCKS / FUN-MANNEQUIN-TOGGLE
package com.openkitmaker.android.render

import android.graphics.Bitmap
import android.opengl.Matrix
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.google.android.filament.LightManager
import com.google.android.filament.Texture
import com.openkitmaker.android.domain.FabricType
import com.openkitmaker.android.domain.LightingMode
import com.openkitmaker.android.domain.ViewportBackground
import com.openkitmaker.android.ui.editor.GarmentSelection
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renderer 3D PBR hardware-accelerated tramite Google Filament / SceneView.
 *
 * Supporta:
 * - 60 FPS stabili
 * - Camera orbitale 360° con pitch vincolato [-35°, +45°]
 * - Multi-Mesh: isolamento automatico dei nodi Jersey, Shorts, Socks in base al capo attivo
 * - Toggle visibilità Manichino
 * - PBR Sportswear: roughness 0.8, metalness 0.0, normal map procedurale micro-weave
 */
@Composable
fun Kit3DRenderer(
    albedoBitmap: Bitmap?,
    jerseyBitmap: Bitmap? = albedoBitmap,
    shortsBitmap: Bitmap? = null,
    socksBitmap: Bitmap? = null,
    selectedGarment: GarmentSelection = GarmentSelection.ALL,
    lightingMode: LightingMode,
    viewportBackground: ViewportBackground = ViewportBackground.DARK_SOLID,
    fabricType: FabricType = FabricType.MESH,
    roughness: Float,
    metalness: Float,
    yawDegrees: Float,
    pitchDegrees: Float,
    zoomLevel: Float,
    showMannequin: Boolean = true,
    showAnatomicalView: Boolean = false,
    onOrbitChange: (deltaYaw: Float, deltaPitch: Float, zoomFactor: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    // Pre-allocazione statica della matrice di trasformazione 4x4
    val cachedTransformMatrix = remember {
        FloatArray(16).apply { Matrix.setIdentityM(this, 0) }
    }

    var currentJerseyTexture by remember { mutableStateOf<Texture?>(null) }
    var currentShortsTexture by remember { mutableStateOf<Texture?>(null) }
    var currentSocksTexture by remember { mutableStateOf<Texture?>(null) }

    val fabricTextures = remember(engine) {
        FabricType.entries.associateWith { type ->
            MaterialFactory.createProceduralFabricNormalTexture(engine, type)
        }
    }
    val activeNormalTexture = fabricTextures[fabricType] ?: fabricTextures.getValue(FabricType.MESH)

    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0.0f, y = 0.1f, z = 2.65f)
    }

    val mainLightNode = rememberMainLightNode(engine) {
        intensity = lightingMode.intensityMultiplier * 95_000f
        LightManager.Builder(LightManager.Type.SUN)
            .color(
                if (lightingMode == LightingMode.STUDIO) 0.98f else 1.0f,
                if (lightingMode == LightingMode.STUDIO) 0.99f else 0.96f,
                1.0f
            )
            .intensity(lightingMode.intensityMultiplier * 95_000f)
            .direction(-0.35f, -0.8f, -0.5f)
            .castShadows(true)
    }

    val childNodes = rememberNodes {
        val modelInstance = runCatching {
            modelLoader.createModelInstance(assetFileLocation = "models/kit_full_outfit_clean.glb")
        }.getOrNull()

        if (modelInstance != null) {
            add(
                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = 1.65f,
                    centerOrigin = Position(0f, 0f, 0f)
                )
            )
        }
    }

    // Aggiornamento reattivo delle texture 2048x2048 su GPU Filament e visibilità multi-mesh
    LaunchedEffect(
        jerseyBitmap,
        shortsBitmap,
        socksBitmap,
        albedoBitmap,
        roughness,
        metalness,
        showMannequin,
        selectedGarment
    ) {
        val jBmp = jerseyBitmap ?: albedoBitmap
        if (jBmp != null) {
            currentJerseyTexture = MaterialFactory.createOrUpdateAlbedoTexture(engine, currentJerseyTexture, jBmp)
        }
        if (shortsBitmap != null) {
            currentShortsTexture = MaterialFactory.createOrUpdateAlbedoTexture(engine, currentShortsTexture, shortsBitmap)
        }
        if (socksBitmap != null) {
            currentSocksTexture = MaterialFactory.createOrUpdateAlbedoTexture(engine, currentSocksTexture, socksBitmap)
        }

        val isJerseyVisible = selectedGarment == GarmentSelection.JERSEY || selectedGarment == GarmentSelection.ALL
        val isShortsVisible = selectedGarment == GarmentSelection.SHORTS || selectedGarment == GarmentSelection.ALL
        val isSocksVisible = selectedGarment == GarmentSelection.SOCKS || selectedGarment == GarmentSelection.ALL
        val isMannequinVisible = showMannequin

        childNodes.filterIsInstance<ModelNode>().forEach { node ->
            node.modelInstance?.entities?.forEach { entity ->
                val rm = engine.renderableManager
                val ri = rm.getInstance(entity)
                if (ri != 0) {
                    val count = rm.getPrimitiveCount(ri)
                    for (p in 0 until count) {
                        val mi = rm.getMaterialInstanceAt(ri, p)
                        val name = runCatching { mi.name.lowercase() }.getOrDefault("")

                        val isVisible = when {
                            name.contains("jersey") -> isJerseyVisible
                            name.contains("shorts") -> isShortsVisible
                            name.contains("socks") -> isSocksVisible
                            name.contains("mannequin") -> isMannequinVisible
                            else -> true
                        }
                        rm.setLayerMask(ri, 0xFF, if (isVisible) 0xFF else 0x00)

                        if (isVisible && !name.contains("mannequin")) {
                            val activeTex = when {
                                name.contains("shorts") -> currentShortsTexture ?: currentJerseyTexture
                                name.contains("socks") -> currentSocksTexture ?: currentJerseyTexture
                                else -> currentJerseyTexture
                            }
                            if (activeTex != null) {
                                MaterialFactory.applyKitPbrParameters(
                                    materialInstance = mi,
                                    albedoTexture = activeTex,
                                    normalTexture = activeNormalTexture,
                                    roughness = fabricType.defaultRoughness,
                                    metalness = fabricType.defaultMetalness
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Aggiornamento preset di illuminazione Studio / Stadio
    LaunchedEffect(lightingMode) {
        mainLightNode.intensity = lightingMode.intensityMultiplier * 95_000f
    }

    // Aggiornamento camera orbitale con pitch vincolato [-35°, +45°] e centratura dinamica sul capo attivo
    LaunchedEffect(yawDegrees, pitchDegrees, zoomLevel, selectedGarment) {
        val clampedPitch = pitchDegrees.coerceIn(MIN_ORBIT_PITCH_DEG, MAX_ORBIT_PITCH_DEG)
        val clampedZoom = zoomLevel.coerceIn(0.65f, 2.25f)
        val radius = 2.65f / clampedZoom

        val targetY = when (selectedGarment) {
            GarmentSelection.JERSEY -> 0.40f
            GarmentSelection.SHORTS -> -0.10f
            GarmentSelection.SOCKS -> -0.65f
            GarmentSelection.ALL -> 0.05f
        }

        val yawRad = Math.toRadians(yawDegrees.toDouble())
        val pitchRad = Math.toRadians(clampedPitch.toDouble())

        val camX = (radius * cos(pitchRad) * sin(yawRad)).toFloat()
        val camY = targetY + (radius * sin(pitchRad)).toFloat()
        val camZ = (radius * cos(pitchRad) * cos(yawRad)).toFloat()

        // Aggiorna posizione della camera orbitale
        cameraNode.position = Position(camX, camY, camZ)
        cameraNode.lookAt(Position(0f, targetY, 0f))

        // Mantiene la scala proporzionata
        Matrix.setIdentityM(cachedTransformMatrix, 0)
        Matrix.scaleM(cachedTransformMatrix, 0, 1.55f, 1.55f, 1.55f)
        val tm = engine.transformManager
        childNodes.firstOrNull()?.let { node ->
            val inst = tm.getInstance(node.entity)
            if (inst != 0) {
                tm.setTransform(inst, cachedTransformMatrix)
            }
        }
    }

    DisposableEffect(engine) {
        onDispose {
            currentJerseyTexture?.let { engine.destroyTexture(it) }
            currentShortsTexture?.let { engine.destroyTexture(it) }
            currentSocksTexture?.let { engine.destroyTexture(it) }
            fabricTextures.values.forEach { engine.destroyTexture(it) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(viewportBackground.hexColor))
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            mainLightNode = mainLightNode,
            childNodes = childNodes,
            isOpaque = true
        )

        // Overlay trasparente sopra la SceneView per intercettare stabilmente i gesti touch di rotazione orbitale
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        onOrbitChange(pan.x * 0.40f, -pan.y * 0.30f, zoom)
                    }
                }
        )
    }
}

private const val MIN_ORBIT_PITCH_DEG = -35f
private const val MAX_ORBIT_PITCH_DEG = 45f
