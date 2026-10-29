// Implements: SCR-EDITOR / FUN-MANAGE-LAYERS / FUN-EDIT-COLOR / FUN-COMPOSITE-2D / FUN-PREVIEW-3D / FUN-SAVE-LOCAL
package com.openkitmaker.android.ui.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.openkitmaker.android.domain.PatternStyle
import com.openkitmaker.android.domain.LayerType
import com.openkitmaker.android.domain.FabricType
import com.openkitmaker.android.domain.BlendMode
import com.openkitmaker.android.domain.CameraPreset
import com.openkitmaker.android.domain.EditorViewMode
import com.openkitmaker.android.domain.LightingMode
import com.openkitmaker.android.domain.ViewportBackground
import com.openkitmaker.android.render.Kit3DRenderer
import com.openkitmaker.android.ui.components.ColorPicker
import com.openkitmaker.android.ui.components.LayerList

enum class BottomEditorTab(val label: String) {
    LAYERS("Livelli"),
    PATTERN("Pattern"),
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
    val jerseyBitmap by viewModel.jerseyBitmap.collectAsState()
    val shortsBitmap by viewModel.shortsBitmap.collectAsState()
    val socksBitmap by viewModel.socksBitmap.collectAsState()
    val previewMockupBitmap by viewModel.previewMockupBitmap.collectAsState()
    val yaw by viewModel.yaw.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val zoom by viewModel.zoom.collectAsState()
    val viewportBackground by viewModel.viewportBackground.collectAsState()
    val fabricType by viewModel.fabricType.collectAsState()
    val showAnatomicalView by viewModel.showAnatomicalView.collectAsState()
    val activeCameraPreset by viewModel.activeCameraPreset.collectAsState()
    val autoRotate by viewModel.autoRotate.collectAsState()

    val animatedYaw by animateFloatAsState(targetValue = yaw, label = "animatedYaw")
    val animatedPitch by animateFloatAsState(targetValue = pitch, label = "animatedPitch")

    var activeTab by remember { mutableStateOf(BottomEditorTab.LAYERS) }
    var showRawUvSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
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
                                text = when (currentKit.metadata.lightingMode) {
                                    LightingMode.STUDIO -> "💡"
                                    LightingMode.STADIUM -> "🏟️"
                                    LightingMode.SUNSET -> "🌅"
                                    LightingMode.RADIANT -> "✨"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.saveDraft { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
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

                // Tab 2: Pattern Procedurali
                NavigationBarItem(
                    selected = activeTab == BottomEditorTab.PATTERN,
                    onClick = { activeTab = BottomEditorTab.PATTERN },
                    icon = {
                        Text(
                            text = "⚡",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (activeTab == BottomEditorTab.PATTERN) Color(0xFF00C49F) else Color(0xFF94A3B8)
                        )
                    },
                    label = { Text("Pattern") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF00C49F),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF253146)
                    )
                )

                // Tab 3: Colore
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
                        var isDragging by remember { mutableStateOf(false) }
                        var activeSnapGuide by remember { mutableStateOf<String?>(null) }
                        var snapTargetType by remember { mutableStateOf<String?>(null) }

                        val isDraggableLayer = activeLayer?.type == LayerType.SPONSOR || activeLayer?.type == LayerType.BADGE

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp)
                                .then(
                                    if (isDraggableLayer && activeLayer != null) {
                                        Modifier.pointerInput(activeLayer.id) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    isDragging = true
                                                    activeSnapGuide = null
                                                },
                                                onDragEnd = {
                                                    isDragging = false
                                                },
                                                onDragCancel = {
                                                    isDragging = false
                                                    activeSnapGuide = null
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val rawX = activeLayer.properties.offsetX + dragAmount.x * 1.5f
                                                    val rawY = activeLayer.properties.offsetY + dragAmount.y * 1.5f

                                                    var snappedX = rawX
                                                    var snappedY = rawY
                                                    var guide: String? = null
                                                    var target: String? = null

                                                    if (kotlin.math.abs(rawX) < 28f) {
                                                        snappedX = 0f
                                                        guide = "Centro del Petto (X=0)"
                                                        target = "CHEST_CENTER"
                                                    } else if (kotlin.math.abs(rawX - 160f) < 28f) {
                                                        snappedX = 160f
                                                        guide = "Cuore SX"
                                                        target = "CREST_LEFT"
                                                    } else if (kotlin.math.abs(rawX + 160f) < 28f) {
                                                        snappedX = -160f
                                                        guide = "Petto DX"
                                                        target = "CREST_RIGHT"
                                                    }

                                                    if (kotlin.math.abs(rawY - (-140f)) < 28f) {
                                                        snappedY = -140f
                                                        guide = if (guide != null) "$guide + Colletto" else "Altezza Colletto"
                                                        target = target ?: "COLLAR_HEIGHT"
                                                    }

                                                    activeSnapGuide = guide
                                                    snapTargetType = target

                                                    viewModel.updateLayerProperties(activeLayer.id) { props ->
                                                        props.copy(
                                                            offsetX = snappedX,
                                                            offsetY = snappedY,
                                                            snappedToGuide = guide != null
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    } else Modifier
                                ),
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

                            // Magnetic Snapping Guidelines Overlay
                            if (isDragging || activeSnapGuide != null) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f), 0f)

                                    if (snapTargetType == "CHEST_CENTER" || activeSnapGuide?.contains("Centro") == true) {
                                        drawLine(
                                            color = Color(0xFF00C49F),
                                            start = Offset(w * 0.5f, h * 0.15f),
                                            end = Offset(w * 0.5f, h * 0.85f),
                                            strokeWidth = 3f,
                                            pathEffect = dashEffect
                                        )
                                        drawCircle(color = Color(0xFF00C49F), radius = 10f, center = Offset(w * 0.5f, h * 0.5f))
                                    }

                                    if (snapTargetType == "COLLAR_HEIGHT" || activeSnapGuide?.contains("Colletto") == true) {
                                        drawLine(
                                            color = Color(0xFF38BDF8),
                                            start = Offset(w * 0.2f, h * 0.32f),
                                            end = Offset(w * 0.8f, h * 0.32f),
                                            strokeWidth = 3f,
                                            pathEffect = dashEffect
                                        )
                                        drawCircle(color = Color(0xFF38BDF8), radius = 10f, center = Offset(w * 0.5f, h * 0.32f))
                                    }

                                    if (snapTargetType == "CREST_LEFT" || activeSnapGuide?.contains("Cuore") == true) {
                                        drawLine(
                                            color = Color(0xFFF59E0B),
                                            start = Offset(w * 0.65f, h * 0.25f),
                                            end = Offset(w * 0.65f, h * 0.55f),
                                            strokeWidth = 3f,
                                            pathEffect = dashEffect
                                        )
                                        drawCircle(color = Color(0xFFF59E0B), radius = 12f, center = Offset(w * 0.65f, h * 0.40f), style = Stroke(width = 3f))
                                    }

                                    if (snapTargetType == "CREST_RIGHT" || activeSnapGuide?.contains("Petto DX") == true) {
                                        drawLine(
                                            color = Color(0xFFF59E0B),
                                            start = Offset(w * 0.35f, h * 0.25f),
                                            end = Offset(w * 0.35f, h * 0.55f),
                                            strokeWidth = 3f,
                                            pathEffect = dashEffect
                                        )
                                        drawCircle(color = Color(0xFFF59E0B), radius = 12f, center = Offset(w * 0.35f, h * 0.40f), style = Stroke(width = 3f))
                                    }
                                }
                            }

                            // Snapping HUD Banner
                            activeSnapGuide?.let { guideText ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp)
                                        .background(Color(0xE60F141C), RoundedCornerShape(16.dp))
                                        .border(1.5.dp, Color(0xFF00C49F), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("🧲", style = MaterialTheme.typography.bodyMedium)
                                        Text(guideText, color = Color(0xFF00C49F), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0x3300C49F), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("SNAP", color = Color(0xFF00C49F), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }

                            // Snapping status pill
                            if (isDraggableLayer) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                        .background(Color(0xE60F141C), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF2E3D56), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("🧲 Snapping Attivo · Trascina", color = Color(0xFF00C49F), style = MaterialTheme.typography.labelSmall)
                                }
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
                                jerseyBitmap = jerseyBitmap,
                                shortsBitmap = shortsBitmap,
                                socksBitmap = socksBitmap,
                                selectedGarment = activeGarmentType,
                                lightingMode = currentKit.metadata.lightingMode,
                                viewportBackground = viewportBackground,
                                fabricType = fabricType,
                                roughness = fabricType.defaultRoughness,
                                metalness = fabricType.defaultMetalness,
                                yawDegrees = animatedYaw,
                                pitchDegrees = animatedPitch,
                                zoomLevel = zoom,
                                showMannequin = showMannequin,
                                showAnatomicalView = showAnatomicalView,
                                onOrbitChange = viewModel::updateOrbit
                            )

                            // Floating Control Bar above 3D Viewport (Material Design 3 Glassmorphism)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .align(Alignment.TopCenter)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF141B26).copy(alpha = 0.88f), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFF2E3D56).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Camera Presets
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFF0F141C).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CameraPreset.entries.forEach { preset ->
                                            val isSelected = activeCameraPreset == preset && !autoRotate
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.setCameraPreset(preset) },
                                                label = { Text(preset.labelIt, style = MaterialTheme.typography.labelSmall) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF0F52BA),
                                                    selectedLabelColor = Color.White,
                                                    containerColor = Color.Transparent,
                                                    labelColor = Color(0xFF94A3B8)
                                                )
                                            )
                                        }
                                        FilterChip(
                                            selected = autoRotate,
                                            onClick = viewModel::toggleAutoRotate,
                                            label = { Text("🔄 360°", style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF00C49F),
                                                selectedLabelColor = Color(0xFF0F141C),
                                                containerColor = Color.Transparent,
                                                labelColor = Color(0xFF94A3B8)
                                            )
                                        )
                                    }

                                    // 2. Lighting Presets
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFF0F141C).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LightingMode.entries.forEach { mode ->
                                            val isSelected = currentKit.metadata.lightingMode == mode
                                            val emoji = when (mode) {
                                                LightingMode.STUDIO -> "💡"
                                                LightingMode.STADIUM -> "🏟️"
                                                LightingMode.SUNSET -> "🌅"
                                                LightingMode.RADIANT -> "✨"
                                            }
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.setLightingMode(mode) },
                                                label = { Text("$emoji ${mode.labelIt.substringBefore(" (")}", style = MaterialTheme.typography.labelSmall) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF0F52BA),
                                                    selectedLabelColor = Color.White,
                                                    containerColor = Color.Transparent,
                                                    labelColor = Color(0xFF94A3B8)
                                                )
                                            )
                                        }
                                    }

                                    // 3. Mannequin & Anatomical View Toggles
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFF0F141C).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilterChip(
                                            selected = showMannequin,
                                            onClick = viewModel::toggleMannequin,
                                            label = { Text("👤 Manichino", style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF253146),
                                                selectedLabelColor = Color(0xFF00C49F),
                                                containerColor = Color.Transparent,
                                                labelColor = Color(0xFF64748B)
                                            )
                                        )
                                        FilterChip(
                                            selected = showAnatomicalView,
                                            onClick = viewModel::toggleAnatomicalView,
                                            label = { Text("✂️ Anatomica", style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                                selectedLabelColor = Color(0xFF00E5FF),
                                                containerColor = Color.Transparent,
                                                labelColor = Color(0xFF64748B)
                                            )
                                        )
                                    }

                                    // 4. Background Presets
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFF0F141C).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ViewportBackground.entries.forEach { bg ->
                                            val isSelected = viewportBackground == bg
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.setViewportBackground(bg) },
                                                label = { Text("${bg.emoji} ${bg.labelIt}", style = MaterialTheme.typography.labelSmall) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF253146),
                                                    selectedLabelColor = Color.White,
                                                    containerColor = Color.Transparent,
                                                    labelColor = Color(0xFF94A3B8)
                                                )
                                            )
                                        }
                                    }

                                    // 5. Fabric Presets (Material Engine)
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFF0F141C).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FabricType.entries.forEach { fabric ->
                                            val isSelected = fabricType == fabric
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.setFabricType(fabric) },
                                                label = { Text("${fabric.emoji} ${fabric.labelIt}", style = MaterialTheme.typography.labelSmall) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF00C49F),
                                                    selectedLabelColor = Color(0xFF0F141C),
                                                    containerColor = Color.Transparent,
                                                    labelColor = Color(0xFF94A3B8)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
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

                        BottomEditorTab.PATTERN -> {
                            val patternLayer = activeLayers.firstOrNull { it.id == selectedLayerId && it.type == LayerType.PATTERN }
                                ?: activeLayers.firstOrNull { it.type == LayerType.PATTERN }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF253146)),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚡📐 Generatore Pattern",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0x3300C49F), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("4 Algoritmi", color = Color(0xFF00C49F), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        if (patternLayer != null) {
                                            Text(
                                                text = "Seleziona Algoritmo:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8)
                                            )

                                            val algorithms = listOf(
                                                Triple(PatternStyle.HEXAGONS, "⬡ Esagoni", "Griglia Matematica"),
                                                Triple(PatternStyle.WAVES, "〰 Onde", "Sinusoidale"),
                                                Triple(PatternStyle.CAMOUFLAGE, "🫧 Mimetico", "Blob Organici"),
                                                Triple(PatternStyle.ABSTRACT_GEOMETRIC, "📐 Geometria", "Linee & Tagli")
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                algorithms.take(2).forEach { (style, title, subtitle) ->
                                                    val isCurrent = patternLayer.properties.patternStyle == style
                                                    ElevatedCard(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                viewModel.updateLayerProperties(patternLayer.id) {
                                                                    it.copy(patternStyle = style)
                                                                }
                                                            },
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.elevatedCardColors(
                                                            containerColor = if (isCurrent) Color(0xFF0F52BA) else Color(0xFF1A2230)
                                                        )
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text(title, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelMedium)
                                                            Text(subtitle, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                algorithms.drop(2).forEach { (style, title, subtitle) ->
                                                    val isCurrent = patternLayer.properties.patternStyle == style
                                                    ElevatedCard(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                viewModel.updateLayerProperties(patternLayer.id) {
                                                                    it.copy(patternStyle = style)
                                                                }
                                                            },
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.elevatedCardColors(
                                                            containerColor = if (isCurrent) Color(0xFF0F52BA) else Color(0xFF1A2230)
                                                        )
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text(title, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelMedium)
                                                            Text(subtitle, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Scala Pattern: ${String.format(java.util.Locale.US, "%.1f", patternLayer.properties.patternScale)}x",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                            androidx.compose.material3.Slider(
                                                value = patternLayer.properties.patternScale,
                                                onValueChange = { newVal ->
                                                    viewModel.updateLayerProperties(patternLayer.id) { it.copy(patternScale = newVal) }
                                                },
                                                valueRange = 0.2f..3.0f
                                            )

                                            Text(
                                                text = "Rotazione Pattern: ${patternLayer.properties.patternRotation.toInt()}°",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                            androidx.compose.material3.Slider(
                                                value = patternLayer.properties.patternRotation,
                                                onValueChange = { newVal ->
                                                    viewModel.updateLayerProperties(patternLayer.id) { it.copy(patternRotation = newVal) }
                                                },
                                                valueRange = 0f..360f
                                            )

                                            Text(
                                                text = "Frequenza: ${patternLayer.properties.patternFrequency}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                            androidx.compose.material3.Slider(
                                                value = patternLayer.properties.patternFrequency.toFloat(),
                                                onValueChange = { newVal ->
                                                    viewModel.updateLayerProperties(patternLayer.id) { it.copy(patternFrequency = newVal.toInt()) }
                                                },
                                                valueRange = 2f..20f
                                            )
                                        } else {
                                            Text(
                                                text = "Nessun livello pattern presente nel capo attuale. Aggiungine uno dalla scheda Livelli.",
                                                color = Color(0xFF94A3B8),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
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
                                    shape = RoundedCornerShape(16.dp),
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

                                        Text(
                                            text = "Preset Luce:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LightingMode.entries.forEach { mode ->
                                                FilterChip(
                                                    selected = currentKit.metadata.lightingMode == mode,
                                                    onClick = { viewModel.setLightingMode(mode) },
                                                    label = { Text(mode.labelIt.substringBefore(" ("), style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Sfondo 3D:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ViewportBackground.entries.forEach { bg ->
                                                FilterChip(
                                                    selected = viewportBackground == bg,
                                                    onClick = { viewModel.setViewportBackground(bg) },
                                                    label = { Text("${bg.emoji} ${bg.labelIt}", style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            FilterChip(
                                                selected = showMannequin,
                                                onClick = viewModel::toggleMannequin,
                                                label = { Text("👤 Manichino", style = MaterialTheme.typography.labelSmall) }
                                            )
                                            FilterChip(
                                                selected = showAnatomicalView,
                                                onClick = viewModel::toggleAnatomicalView,
                                                label = { Text("✂️ Vista Anatomica", style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }

                                        Text(
                                            text = "Tessuto & Materiale PBR:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FabricType.entries.forEach { fabric ->
                                                FilterChip(
                                                    selected = fabricType == fabric,
                                                    onClick = { viewModel.setFabricType(fabric) },
                                                    label = { Text("${fabric.emoji} ${fabric.labelIt}", style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${fabricType.description} (Roughness: ${fabricType.defaultRoughness}, Metalness: ${fabricType.defaultMetalness})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF00C49F)
                                        )

                                        OutlinedButton(
                                            onClick = { viewModel.setCameraPreset(CameraPreset.FRONT) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Centra Camera Frontale", color = Color.White)
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Usa la barra flottante sul 3D per viste rapide (Fronte, Retro, Lati, 360°) o trascina per esplorare la divisa.",
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
