// Implements: SCR-EDITOR / FUN-MANAGE-LAYERS / FUN-EDIT-COLOR / FUN-COMPOSITE-2D / FUN-PREVIEW-3D / FUN-SAVE-LOCAL
package com.openkitmaker.android.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openkitmaker.android.data.repository.KitRepository
import com.openkitmaker.android.domain.BlendMode
import com.openkitmaker.android.domain.CameraPreset
import com.openkitmaker.android.domain.EditorViewMode
import com.openkitmaker.android.domain.FabricType
import com.openkitmaker.android.domain.Kit
import com.openkitmaker.android.domain.Layer
import com.openkitmaker.android.domain.LightingMode
import com.openkitmaker.android.domain.ViewportBackground
import com.openkitmaker.android.render.TextureEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class GarmentSelection(val labelIt: String, val emoji: String) {
    JERSEY("Maglia", "👕"),
    SHORTS("Pantaloncini", "🩳"),
    SOCKS("Calzettoni", "🧦"),
    ALL("Vista Completo", "👤")
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val kitRepository: KitRepository
) : ViewModel() {

    private val kitId: String = checkNotNull(savedStateHandle["kitId"])

    private val _kit = MutableStateFlow<Kit?>(null)
    val kit: StateFlow<Kit?> = _kit.asStateFlow()

    private val _activeGarmentType = MutableStateFlow(GarmentSelection.JERSEY)
    val activeGarmentType: StateFlow<GarmentSelection> = _activeGarmentType.asStateFlow()

    private val _showMannequin = MutableStateFlow(true)
    val showMannequin: StateFlow<Boolean> = _showMannequin.asStateFlow()

    private val _showAnatomicalView = MutableStateFlow(false)
    val showAnatomicalView: StateFlow<Boolean> = _showAnatomicalView.asStateFlow()

    private val _viewportBackground = MutableStateFlow(ViewportBackground.DARK_SOLID)
    val viewportBackground: StateFlow<ViewportBackground> = _viewportBackground.asStateFlow()

    private val _fabricType = MutableStateFlow(FabricType.MESH)
    val fabricType: StateFlow<FabricType> = _fabricType.asStateFlow()

    private val _activeCameraPreset = MutableStateFlow<CameraPreset?>(CameraPreset.FRONT)
    val activeCameraPreset: StateFlow<CameraPreset?> = _activeCameraPreset.asStateFlow()

    private val _autoRotate = MutableStateFlow(false)
    val autoRotate: StateFlow<Boolean> = _autoRotate.asStateFlow()

    private var autoRotateJob: Job? = null

    private val _selectedLayerId = MutableStateFlow<String?>(null)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    private val _viewMode = MutableStateFlow(EditorViewMode.VIEW_SPLIT)
    val viewMode: StateFlow<EditorViewMode> = _viewMode.asStateFlow()

    private val _jerseyBitmap = MutableStateFlow<Bitmap?>(null)
    val jerseyBitmap: StateFlow<Bitmap?> = _jerseyBitmap.asStateFlow()

    private val _shortsBitmap = MutableStateFlow<Bitmap?>(null)
    val shortsBitmap: StateFlow<Bitmap?> = _shortsBitmap.asStateFlow()

    private val _socksBitmap = MutableStateFlow<Bitmap?>(null)
    val socksBitmap: StateFlow<Bitmap?> = _socksBitmap.asStateFlow()

    private val _albedoBitmap = MutableStateFlow<Bitmap?>(null)
    val albedoBitmap: StateFlow<Bitmap?> = _albedoBitmap.asStateFlow()

    private val _previewMockupBitmap = MutableStateFlow<Bitmap?>(null)
    val previewMockupBitmap: StateFlow<Bitmap?> = _previewMockupBitmap.asStateFlow()

    private val _yaw = MutableStateFlow(0f)
    val yaw: StateFlow<Float> = _yaw.asStateFlow()

    private val _pitch = MutableStateFlow(8f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _zoom = MutableStateFlow(1.0f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = kitRepository.getKitById(kitId)
            if (loaded != null) {
                _kit.value = loaded
                _viewportBackground.value = loaded.metadata.viewportBackground
                _showAnatomicalView.value = loaded.metadata.showAnatomicalView
                _fabricType.value = loaded.metadata.fabricType
                _selectedLayerId.value = loaded.garments.jersey.layers.firstOrNull()?.id
                recomposite2D(loaded)
            }
        }
    }

    fun selectGarment(selection: GarmentSelection) {
        _activeGarmentType.value = selection
        val current = _kit.value ?: return
        if (selection != GarmentSelection.ALL) {
            val layers = getActiveLayers(current, selection)
            _selectedLayerId.value = layers.firstOrNull()?.id
        }
        recomposite2D(current)
    }

    fun toggleMannequin() {
        _showMannequin.update { !it }
    }

    fun getActiveLayers(currentKit: Kit, selection: GarmentSelection = _activeGarmentType.value): List<Layer> {
        return when (selection) {
            GarmentSelection.JERSEY -> currentKit.garments.jersey.layers
            GarmentSelection.SHORTS -> currentKit.garments.shorts.layers
            GarmentSelection.SOCKS -> currentKit.garments.socks.layers
            GarmentSelection.ALL -> currentKit.layers
        }
    }

    private fun recomposite2D(currentKit: Kit) {
        viewModelScope.launch(Dispatchers.Default) {
            val selection = _activeGarmentType.value
            val jerseyBmp = TextureEngine.compositeJerseyBitmap(currentKit, 2048, 2048)
            val shortsBmp = TextureEngine.compositeShortsBitmap(currentKit, 2048, 2048)
            val socksBmp = TextureEngine.compositeSocksBitmap(currentKit, 2048, 2048)

            val activeBmp = when (selection) {
                GarmentSelection.JERSEY -> jerseyBmp
                GarmentSelection.SHORTS -> shortsBmp
                GarmentSelection.SOCKS -> socksBmp
                GarmentSelection.ALL -> jerseyBmp
            }

            val mockup = when (selection) {
                GarmentSelection.JERSEY -> TextureEngine.renderJerseyMockupBitmap(currentKit, jerseyBmp, 1024, 1024)
                GarmentSelection.SHORTS -> TextureEngine.renderShortsMockupBitmap(currentKit, shortsBmp, 1024, 1024)
                GarmentSelection.SOCKS -> TextureEngine.renderSocksMockupBitmap(currentKit, socksBmp, 1024, 1024)
                GarmentSelection.ALL -> TextureEngine.renderFullKitMockupBitmap(currentKit, 1024, 1024)
            }

            withContext(Dispatchers.Main) {
                _jerseyBitmap.value = jerseyBmp
                _shortsBitmap.value = shortsBmp
                _socksBitmap.value = socksBmp
                _albedoBitmap.value = activeBmp
                _previewMockupBitmap.value = mockup
            }
        }
    }

    fun setViewMode(mode: EditorViewMode) {
        _viewMode.value = mode
    }

    fun setLightingMode(mode: LightingMode) {
        val current = _kit.value ?: return
        val updated = current.copy(metadata = current.metadata.copy(lightingMode = mode))
        _kit.value = updated
    }

    fun setViewportBackground(bg: ViewportBackground) {
        _viewportBackground.value = bg
        val current = _kit.value ?: return
        val updated = current.copy(metadata = current.metadata.copy(viewportBackground = bg))
        _kit.value = updated
    }

    fun setFabricType(fabric: FabricType) {
        _fabricType.value = fabric
        val current = _kit.value ?: return
        val updated = current.copy(
            metadata = current.metadata.copy(
                fabricType = fabric,
                roughness = fabric.defaultRoughness,
                metalness = fabric.defaultMetalness
            )
        )
        _kit.value = updated
    }

    fun toggleAnatomicalView() {
        val next = !_showAnatomicalView.value
        _showAnatomicalView.value = next
        val current = _kit.value ?: return
        val updated = current.copy(metadata = current.metadata.copy(showAnatomicalView = next))
        _kit.value = updated
        recomposite2D(updated)
    }

    fun setCameraPreset(preset: CameraPreset) {
        if (_autoRotate.value) {
            toggleAutoRotate()
        }
        _activeCameraPreset.value = preset
        when (preset) {
            CameraPreset.FRONT -> {
                _yaw.value = 0f
                _pitch.value = 8f
            }
            CameraPreset.BACK -> {
                _yaw.value = 180f
                _pitch.value = 8f
            }
            CameraPreset.LEFT -> {
                _yaw.value = 90f
                _pitch.value = 8f
            }
            CameraPreset.RIGHT -> {
                _yaw.value = 270f
                _pitch.value = 8f
            }
        }
    }

    fun toggleAutoRotate() {
        val next = !_autoRotate.value
        _autoRotate.value = next
        if (next) {
            _activeCameraPreset.value = null
            autoRotateJob = viewModelScope.launch {
                while (_autoRotate.value) {
                    _yaw.update { (it + 0.8f) % 360f }
                    delay(16)
                }
            }
        } else {
            autoRotateJob?.cancel()
            autoRotateJob = null
        }
    }

    fun toggleLightingMode() {
        val current = _kit.value ?: return
        val modes = LightingMode.entries
        val nextIdx = (modes.indexOf(current.metadata.lightingMode) + 1) % modes.size
        val nextLight = modes[nextIdx]
        val updated = current.copy(metadata = current.metadata.copy(lightingMode = nextLight))
        _kit.value = updated
    }

    fun updateOrbit(deltaYaw: Float, deltaPitch: Float, zoomFactor: Float) {
        if (_autoRotate.value) {
            toggleAutoRotate()
        }
        _activeCameraPreset.value = null
        _yaw.update { (it + deltaYaw) % 360f }
        _pitch.update { (it + deltaPitch).coerceIn(-35f, 45f) }
        _zoom.update { (it * zoomFactor).coerceIn(0.65f, 2.25f) }
    }

    fun selectLayer(id: String) {
        _selectedLayerId.value = id
    }

    fun updateKitName(newName: String) {
        val current = _kit.value ?: return
        _kit.value = current.copy(name = newName)
    }

    fun updateLayerColor(newHex: String) {
        val current = _kit.value ?: return
        val targetId = _selectedLayerId.value ?: return
        val selection = _activeGarmentType.value

        val updatedGarments = when (selection) {
            GarmentSelection.JERSEY -> {
                val updatedLayers = current.garments.jersey.layers.map {
                    if (it.id == targetId) it.copy(color = newHex) else it
                }
                current.garments.copy(jersey = current.garments.jersey.copy(layers = updatedLayers))
            }
            GarmentSelection.SHORTS -> {
                val updatedLayers = current.garments.shorts.layers.map {
                    if (it.id == targetId) it.copy(color = newHex) else it
                }
                current.garments.copy(shorts = current.garments.shorts.copy(layers = updatedLayers))
            }
            GarmentSelection.SOCKS -> {
                val updatedLayers = current.garments.socks.layers.map {
                    if (it.id == targetId) it.copy(color = newHex) else it
                }
                current.garments.copy(socks = current.garments.socks.copy(layers = updatedLayers))
            }
            GarmentSelection.ALL -> current.garments
        }

        val updatedRecent = (listOf(newHex) + current.metadata.recentColors)
            .distinct()
            .take(8)
        val updatedKit = current.copy(
            garments = updatedGarments,
            layers = updatedGarments.jersey.layers,
            metadata = current.metadata.copy(recentColors = updatedRecent)
        )
        _kit.value = updatedKit
        recomposite2D(updatedKit)
    }

    fun toggleLayerVisibility(id: String) {
        mutateLayer(id) { it.copy(visible = !it.visible) }
    }

    fun updateLayerOpacity(id: String, opacity: Float) {
        mutateLayer(id) { it.copy(opacity = opacity.coerceIn(0f, 1f)) }
    }

    fun updateLayerBlendMode(id: String, mode: BlendMode) {
        mutateLayer(id) { it.copy(blendMode = mode) }
    }

    fun updateLayerProperties(id: String, transform: (com.openkitmaker.android.domain.LayerProperties) -> com.openkitmaker.android.domain.LayerProperties) {
        mutateLayer(id) { it.copy(properties = transform(it.properties)) }
    }

    fun moveLayerZIndex(id: String, direction: Int) {
        val current = _kit.value ?: return
        val selection = _activeGarmentType.value
        val layers = getActiveLayers(current, selection)
        val sorted = layers.sortedBy { it.zIndex }.toMutableList()
        val idx = sorted.indexOfFirst { it.id == id }
        val targetIdx = (idx + direction).coerceIn(0, sorted.lastIndex)
        if (idx != -1 && idx != targetIdx) {
            val item = sorted.removeAt(idx)
            sorted.add(targetIdx, item)
            val reindexed = sorted.mapIndexed { index, layer -> layer.copy(zIndex = index) }
            val updatedGarments = when (selection) {
                GarmentSelection.JERSEY -> current.garments.copy(jersey = current.garments.jersey.copy(layers = reindexed))
                GarmentSelection.SHORTS -> current.garments.copy(shorts = current.garments.shorts.copy(layers = reindexed))
                GarmentSelection.SOCKS -> current.garments.copy(socks = current.garments.socks.copy(layers = reindexed))
                GarmentSelection.ALL -> current.garments
            }
            val updated = current.copy(
                garments = updatedGarments,
                layers = updatedGarments.jersey.layers
            )
            _kit.value = updated
            recomposite2D(updated)
        }
    }

    private fun mutateLayer(id: String, block: (Layer) -> Layer) {
        val current = _kit.value ?: return
        val selection = _activeGarmentType.value

        val updatedGarments = when (selection) {
            GarmentSelection.JERSEY -> {
                val updatedLayers = current.garments.jersey.layers.map { if (it.id == id) block(it) else it }
                current.garments.copy(jersey = current.garments.jersey.copy(layers = updatedLayers))
            }
            GarmentSelection.SHORTS -> {
                val updatedLayers = current.garments.shorts.layers.map { if (it.id == id) block(it) else it }
                current.garments.copy(shorts = current.garments.shorts.copy(layers = updatedLayers))
            }
            GarmentSelection.SOCKS -> {
                val updatedLayers = current.garments.socks.layers.map { if (it.id == id) block(it) else it }
                current.garments.copy(socks = current.garments.socks.copy(layers = updatedLayers))
            }
            GarmentSelection.ALL -> current.garments
        }

        val updated = current.copy(
            garments = updatedGarments,
            layers = updatedGarments.jersey.layers
        )
        _kit.value = updated
        recomposite2D(updated)
    }

    fun saveDraft(onResult: ((Boolean, String) -> Unit)? = null) {
        val current = _kit.value ?: return
        viewModelScope.launch {
            try {
                kitRepository.saveKit(current)
                onResult?.invoke(true, "Kit \"${current.name}\" salvato in memoria locale.")
            } catch (e: Exception) {
                onResult?.invoke(false, "Spazio di archiviazione locale insufficiente per completare l'operazione.")
            }
        }
    }
}
