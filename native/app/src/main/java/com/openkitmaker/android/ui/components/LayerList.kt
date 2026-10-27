// Implements: SCR-EDITOR / FUN-MANAGE-LAYERS
package com.openkitmaker.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

@Composable
fun LayerList(
    layers: List<Layer>,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onMoveLayer: (String, Int) -> Unit,
    onOpacityChange: (String, Float) -> Unit,
    onBlendModeChange: (String, BlendMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        layers.sortedByDescending { it.zIndex }.forEach { layer ->
            val isSelected = layer.id == selectedLayerId
            val layerColor = runCatching { Color(android.graphics.Color.parseColor(layer.color)) }
                .getOrDefault(Color(0xFF0F52BA))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectLayer(layer.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected) Color(0xFF253146) else Color(0xFF1A2230)
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = if (isSelected) 6.dp else 2.dp
                )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(layerColor, CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Checkbox(
                                checked = layer.visible,
                                onCheckedChange = { onToggleVisibility(layer.id) }
                            )
                            Column {
                                Text(
                                    text = layer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${layer.type.labelIt} · Stack z:${layer.zIndex} · ${(layer.opacity * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { onMoveLayer(layer.id, +1) }) {
                                Text("▲ Su", color = Color(0xFF00C49F))
                            }
                            TextButton(onClick = { onMoveLayer(layer.id, -1) }) {
                                Text("▼ Giù", color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    if (isSelected) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F141C), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Opacità Livello",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "${(layer.opacity * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF00C49F)
                                )
                            }
                            Slider(
                                value = layer.opacity,
                                onValueChange = { onOpacityChange(layer.id, it) },
                                valueRange = 0f..1f
                            )

                            Text(
                                text = "Metodo Fusione (Blend Mode)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BlendMode.entries.forEach { mode ->
                                    val isCurrentMode = layer.blendMode == mode
                                    TextButton(
                                        onClick = { onBlendModeChange(layer.id, mode) }
                                    ) {
                                        Text(
                                            text = mode.labelIt,
                                            fontWeight = if (isCurrentMode) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrentMode) Color(0xFF00C49F) else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
