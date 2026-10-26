// Implements: SCR-EDITOR / FUN-MANAGE-LAYERS / FUN-EDIT-COLOR / FUN-COMPOSITE-2D / FUN-PREVIEW-3D / FUN-SAVE-LOCAL
package com.openkitmaker.native.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openkitmaker.native.domain.EditorViewMode
import com.openkitmaker.native.render.Kit3DRenderer
import com.openkitmaker.native.ui.components.ColorPicker
import com.openkitmaker.native.ui.components.LayerList

@Composable
fun EditorScreen(
    onNavigateToExport: (String) -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val kit by viewModel.kit.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val albedoBitmap by viewModel.albedoBitmap.collectAsState()
    val yaw by viewModel.yaw.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val zoom by viewModel.zoom.collectAsState()

    val currentKit = kit ?: return
    val activeLayer = currentKit.layers.firstOrNull { it.id == selectedLayerId }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A2230))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentKit.name,
                onValueChange = viewModel::updateKitName,
                singleLine = true,
                label = { Text("Titolo Kit") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorViewMode.entries.forEach { mode ->
                    OutlinedButton(onClick = { viewModel.setViewMode(mode) }) {
                        Text(mode.labelIt)
                    }
                }
                OutlinedButton(onClick = viewModel::toggleLightingMode) {
                    Text(currentKit.metadata.lightingMode.labelIt)
                }
                Button(onClick = viewModel::saveDraft) {
                    Text("Salva Bozza")
                }
                Button(onClick = {
                    viewModel.saveDraft()
                    onNavigateToExport(currentKit.id)
                }) {
                    Text("Esporta")
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
                    .background(Color(0xFF0F141C))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (viewMode == EditorViewMode.VIEW_2D || viewMode == EditorViewMode.VIEW_SPLIT) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            albedoBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Texture 2D 2048x2048",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    if (viewMode == EditorViewMode.VIEW_3D || viewMode == EditorViewMode.VIEW_SPLIT) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Kit3DRenderer(
                                albedoBitmap = albedoBitmap,
                                lightingMode = currentKit.metadata.lightingMode,
                                roughness = currentKit.metadata.roughness,
                                metalness = currentKit.metadata.metalness,
                                yawDegrees = yaw,
                                pitchDegrees = pitch,
                                zoomLevel = zoom,
                                onOrbitChange = viewModel::updateOrbit
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeLayer != null) {
                    ColorPicker(
                        label = "Colore Livello (${activeLayer.name})",
                        currentHex = activeLayer.color,
                        recentColors = currentKit.metadata.recentColors,
                        onColorChanged = viewModel::updateLayerColor
                    )
                }

                LayerList(
                    layers = currentKit.layers,
                    selectedLayerId = selectedLayerId,
                    onSelectLayer = viewModel::selectLayer,
                    onToggleVisibility = viewModel::toggleLayerVisibility,
                    onMoveLayer = viewModel::moveLayerZIndex,
                    onOpacityChange = viewModel::updateLayerOpacity,
                    onBlendModeChange = viewModel::updateLayerBlendMode
                )
            }
        }
    }
}
