// Implements: SCR-EDITOR / FUN-MANAGE-LAYERS / FUN-EDIT-COLOR / FUN-COMPOSITE-2D / FUN-PREVIEW-3D / FUN-SAVE-LOCAL
package com.openkitmaker.native.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openkitmaker.native.data.repository.KitRepository
import com.openkitmaker.native.domain.BlendMode
import com.openkitmaker.native.domain.EditorViewMode
import com.openkitmaker.native.domain.Kit
import com.openkitmaker.native.domain.Layer
import com.openkitmaker.native.domain.LightingMode
import com.openkitmaker.native.render.TextureEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val kitRepository: KitRepository
) : ViewModel() {

    private val kitId: String = checkNotNull(savedStateHandle["kitId"])

    private val _kit = MutableStateFlow<Kit?>(null)
    val kit: StateFlow<Kit?> = _kit.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<String?>(null)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    private val _viewMode = MutableStateFlow(EditorViewMode.VIEW_SPLIT)
    val viewMode: StateFlow<EditorViewMode> = _viewMode.asStateFlow()

    private val _albedoBitmap = MutableStateFlow<Bitmap?>(null)
    val albedoBitmap: StateFlow<Bitmap?> = _albedoBitmap.asStateFlow()

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
                _selectedLayerId.value = loaded.layers.firstOrNull()?.id
                recomposite2D(loaded)
            }
        }
    }

    private fun recomposite2D(currentKit: Kit) {
        viewModelScope.launch(Dispatchers.Default) {
            val bmp = TextureEngine.compositeKitBitmap(currentKit, 2048, 2048)
            withContext(Dispatchers.Main) {
                _albedoBitmap.value = bmp
            }
        }
    }

    fun setViewMode(mode: EditorViewMode) {
        _viewMode.value = mode
    }

    fun toggleLightingMode() {
        val current = _kit.value ?: return
        val nextLight = if (current.metadata.lightingMode == LightingMode.STUDIO) {
            LightingMode.STADIUM
        } else {
            LightingMode.STUDIO
        }
        val updated = current.copy(metadata = current.metadata.copy(lightingMode = nextLight))
        _kit.value = updated
    }

    fun updateOrbit(deltaYaw: Float, deltaPitch: Float, zoomFactor: Float) {
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
        val updatedLayers = current.layers.map {
            if (it.id == targetId) it.copy(color = newHex) else it
        }
        val updatedRecent = (listOf(newHex) + current.metadata.recentColors)
            .distinct()
            .take(8)
        val updatedKit = current.copy(
            layers = updatedLayers,
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

    fun moveLayerZIndex(id: String, direction: Int) {
        val current = _kit.value ?: return
        val sorted = current.layers.sortedBy { it.zIndex }.toMutableList()
        val idx = sorted.indexOfFirst { it.id == id }
        val targetIdx = (idx + direction).coerceIn(0, sorted.lastIndex)
        if (idx != -1 && idx != targetIdx) {
            val item = sorted.removeAt(idx)
            sorted.add(targetIdx, item)
            val reindexed = sorted.mapIndexed { index, layer -> layer.copy(zIndex = index) }
            val updated = current.copy(layers = reindexed)
            _kit.value = updated
            recomposite2D(updated)
        }
    }

    private fun mutateLayer(id: String, block: (Layer) -> Layer) {
        val current = _kit.value ?: return
        val updated = current.copy(
            layers = current.layers.map { if (it.id == id) block(it) else it }
        )
        _kit.value = updated
        recomposite2D(updated)
    }

    fun saveDraft() {
        val current = _kit.value ?: return
        viewModelScope.launch {
            kitRepository.saveKit(current)
        }
    }
}
