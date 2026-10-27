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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.openkitmaker.android.domain.LightingMode
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
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToCatalog: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    viewModel: EditorViewModel = hiltViewModel()
) {
    val kit by viewModel.kit.collectAsState()
    val activeGarmentType by viewModel.activeGarmentType.collectAsState()
    val showMannequin by viewModel.showMannequin.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val albedoBitmap by viewModel.albedoBitmap.collectAsState()
    val previewMockupBitmap by viewModel.previewMockupBitmap.collectAsState()
    val yaw by viewModel.yaw.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val zoom by viewModel.zoom.collectAsState()

    var activeTab by remember { mutableStateOf(BottomEditorTab.LAYERS) }
    var showRawUvSheet by remember { mutableStateOf(false) }

    val currentKit = kit ?: return
    val activeLayers = viewModel.getActiveLayers(currentKit, activeGarmentType)
    val activeLayer = activeLayers.firstOrNull { it.id == selectedLayerId }
        ?: activeLayers.firstOrNull()

    val activeColorInt = runCatching {
        Color(android.graphics.Color.parseColor(activeLayer?.color ?: "#0F52BA"))
    }.getOrDefault(Color(0xFF0F52BA))

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = "Kit Maker",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00C49F)
                        )
                        OutlinedTextField(
                            value = currentKit.name,
                            onValueChange = viewModel::updateKitName,
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141B26),
                                unfocusedContainerColor = Color(0xFF141B26),
                                focusedBorderColor = Color(0xFF0F52BA),
                                unfocusedBorderColor = Color(0xFF2E3D56)
                            )
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::toggleLightingMode) {
                            Text(
                                text = if (currentKit.metadata.lightingMode == LightingMode.STUDIO) "💡" else "🏟️",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Button(
                            onClick = viewModel::saveDraft,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F52BA)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Salva", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Text("⋮", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color(0xFF1A2230))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🏠 Home", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToHome()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📁 Catalogo", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToCatalog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("💾 I Miei Kit", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToSaved()
                                    }
                                )
                            }
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
                    label = { Text("Livelli (${activeLayers.size})") },
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
            // Garment Selector Bar & 3D Mannequin Toggle (FASE 3 / FUN-EDIT-SHORTS / FUN-EDIT-SOCKS / FUN-MANNEQUIN-TOGGLE)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101622))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GarmentSelection.entries.forEach { selection ->
                        val isSelected = activeGarmentType == selection
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectGarment(selection) },
                            label = {
                                Text(
                                    text = "${selection.emoji} ${selection.labelIt}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (selection == GarmentSelection.ALL) Color(0xFF00C49F) else Color(0xFF0F52BA),
                                selectedLabelColor = if (selection == GarmentSelection.ALL) Color(0xFF0F141C) else Color.White,
                                containerColor = Color(0xFF1A2230),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                // Mannequin Toggle
                FilterChip(
                    selected = showMannequin,
                    onClick = viewModel::toggleMannequin,
                    label = {
                        Text(
                            text = if (showMannequin) "👤 Manichino" else "👤 Solo Capo",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF253146),
                        selectedLabelColor = Color(0xFF00C49F),
                        containerColor = Color(0xFF141B26),
                        labelColor = Color(0xFF64748B)
                    )
                )
            }

            // Viewport Mode Selector Bar (2D / 3D / Split) + Mockup/UV Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141B26))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditorViewMode.entries.forEach { mode ->
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = { viewModel.setViewMode(mode) },
                            label = { Text(mode.labelIt, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0F52BA),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1A2230),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                if (viewMode == EditorViewMode.VIEW_2D || viewMode == EditorViewMode.VIEW_SPLIT) {
                    FilterChip(
                        selected = !showRawUvSheet,
                        onClick = { showRawUvSheet = !showRawUvSheet },
                        label = {
                            Text(
                                text = if (!showRawUvSheet) "Mockup 2D" else "Foglio UV",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF253146),
                            selectedLabelColor = Color(0xFF00C49F),
                            containerColor = Color(0xFF1A2230),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
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
                            val displayBmp = if (showRawUvSheet) albedoBitmap else (previewMockupBitmap ?: albedoBitmap)
                            displayBmp?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = if (showRawUvSheet) "Mappa UV 2048x2048" else "Mockup Maglia 2D",
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
                                showMannequin = showMannequin,
                                onOrbitChange = viewModel::updateOrbit
                            )
                        }
                    }
                }
            }

            // Area Inferiore: Pannello Strumenti dinamico o Vista Completo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (activeGarmentType == GarmentSelection.ALL) 0.35f else 0.45f)
                    .background(Color(0xFF1A2230))
            ) {
                if (activeGarmentType == GarmentSelection.ALL) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "👤 Vista Completo (Full Kit)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Maglia (${currentKit.garments.jersey.layers.size} liv.) · Pantaloncini (${currentKit.garments.shorts.layers.size} liv.) · Calzettoni (${currentKit.garments.socks.layers.size} liv.)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.selectGarment(GarmentSelection.JERSEY) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("← Modifica Capi")
                            }
                            Button(
                                onClick = {
                                    viewModel.saveDraft()
                                    onNavigateToExport(currentKit.id)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C49F))
                            ) {
                                Text("Esporta Completo (ZIP) →", color = Color(0xFF0F141C), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
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
                                        text = "Stack Livelli ${activeGarmentType.labelIt}",
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
                                    layers = activeLayers,
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
}
