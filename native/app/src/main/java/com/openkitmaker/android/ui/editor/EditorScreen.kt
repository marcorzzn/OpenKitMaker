// Implements: SCR-EDITOR / FUN-MANAGE-LAYERS / FUN-EDIT-COLOR / FUN-COMPOSITE-2D / FUN-PREVIEW-3D / FUN-SAVE-LOCAL
package com.openkitmaker.android.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openkitmaker.android.domain.EditorViewMode
import com.openkitmaker.android.render.Kit3DRenderer
import com.openkitmaker.android.ui.components.ColorPicker
import com.openkitmaker.android.ui.components.LayerList

enum class BottomEditorTab(val label: String) {
    LAYERS("Livelli"),
    COLOR("Colore"),
    VIEW("Vista"),
    EXPORT("Esporta")
}

@OptIn(ExperimentalMaterial3Api::class)
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

    var activeTab by remember { mutableStateOf(BottomEditorTab.LAYERS) }

    val currentKit = kit ?: return
    val activeLayer = currentKit.layers.firstOrNull { it.id == selectedLayerId }
        ?: currentKit.layers.firstOrNull()

    val activeColorInt = runCatching {
        Color(android.graphics.Color.parseColor(activeLayer?.color ?: "#0F52BA"))
    }.getOrDefault(Color(0xFF0F52BA))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = currentKit.name,
                        onValueChange = viewModel::updateKitName,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(52.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = viewModel::toggleLightingMode,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "💡 ${currentKit.metadata.lightingMode.labelIt}",
                                color = Color(0xFF00C49F),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Button(
                            onClick = viewModel::saveDraft,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F52BA)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Salva", color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A2230)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1A2230),
                tonalElevation = 8.dp
            ) {
                // Tab 1: Livelli
                NavigationBarItem(
                    selected = activeTab == BottomEditorTab.LAYERS,
                    onClick = { activeTab = BottomEditorTab.LAYERS },
                    icon = {
                        Text(
                            text = "☰",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (activeTab == BottomEditorTab.LAYERS) Color(0xFF00C49F) else Color(0xFF94A3B8)
                        )
                    },
                    label = { Text("Livelli (${currentKit.layers.size})") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF00C49F),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF253146)
                    )
                )

                // Tab 2: Colore
                NavigationBarItem(
                    selected = activeTab == BottomEditorTab.COLOR,
                    onClick = { activeTab = BottomEditorTab.COLOR },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(activeColorInt, CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        )
                    },
                    label = { Text("Colore") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF00C49F),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF253146)
                    )
                )

                // Tab 3: Vista
                NavigationBarItem(
                    selected = activeTab == BottomEditorTab.VIEW,
                    onClick = { activeTab = BottomEditorTab.VIEW },
                    icon = {
                        Text(
                            text = "👁",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (activeTab == BottomEditorTab.VIEW) Color(0xFF00C49F) else Color(0xFF94A3B8)
                        )
                    },
                    label = { Text(viewMode.labelIt) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF00C49F),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF253146)
                    )
                )

                // Tab 4: Esporta
                NavigationBarItem(
                    selected = activeTab == BottomEditorTab.EXPORT,
                    onClick = {
                        viewModel.saveDraft()
                        onNavigateToExport(currentKit.id)
                    },
                    icon = {
                        Text(
                            text = "↗",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF00C49F)
                        )
                    },
                    label = { Text("Esporta", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF00C49F),
                        unselectedTextColor = Color(0xFF00C49F),
                        indicatorColor = Color(0xFF253146)
                    )
                )
            }
        },
        containerColor = Color(0xFF0F141C)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Viewport Mode Selector Bar (2D / 3D / Split)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141B26))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorViewMode.entries.forEach { mode ->
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = { viewModel.setViewMode(mode) },
                            label = { Text(mode.labelIt) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0F52BA),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1A2230),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                Text(
                    text = "2048×2048 RGBA",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }

            // Area Superiore: Viewport 2D Texture e/o 3D PBR Filament
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .background(Color(0xFF0F141C))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (viewMode == EditorViewMode.VIEW_2D || viewMode == EditorViewMode.VIEW_SPLIT) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp),
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

            // Area Inferiore: Pannello Strumenti dinamico basato sul tab attivo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .background(Color(0xFF1A2230))
            ) {
                when (activeTab) {
                    BottomEditorTab.LAYERS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Stack Livelli Maglia",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Trascina o ordina per z-index",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            LayerList(
                                layers = currentKit.layers,
                                selectedLayerId = selectedLayerId,
                                onSelectLayer = { id ->
                                    viewModel.selectLayer(id)
                                },
                                onToggleVisibility = viewModel::toggleLayerVisibility,
                                onMoveLayer = viewModel::moveLayerZIndex,
                                onOpacityChange = viewModel::updateLayerOpacity,
                                onBlendModeChange = viewModel::updateLayerBlendMode
                            )
                        }
                    }

                    BottomEditorTab.COLOR -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (activeLayer != null) {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF253146)),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Colore Primario: ${activeLayer.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        ColorPicker(
                                            label = "Palette HSV Livello (${activeLayer.type.labelIt})",
                                            currentHex = activeLayer.color,
                                            recentColors = currentKit.metadata.recentColors,
                                            onColorChanged = viewModel::updateLayerColor
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Seleziona un livello dalla scheda Livelli per modificarne il colore.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    BottomEditorTab.VIEW -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF253146)),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Impostazioni Studio 3D & Luce",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = viewModel::toggleLightingMode,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Luce: ${currentKit.metadata.lightingMode.labelIt}")
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.updateOrbit(-yaw, -pitch, 1.0f) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Centra Camera", color = Color.White)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Usa pinch-to-zoom per ingrandire e trascina per ruotare la visuale a 360° sulla divisa.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    BottomEditorTab.EXPORT -> {
                        // Handled directly in navigation bar click
                    }
                }
            }
        }
    }
}
