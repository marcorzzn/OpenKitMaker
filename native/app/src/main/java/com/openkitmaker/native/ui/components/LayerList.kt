// Implements: SCR-EDITOR / FUN-MANAGE-LAYERS
package com.openkitmaker.native.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openkitmaker.native.domain.BlendMode
import com.openkitmaker.native.domain.Layer

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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        layers.sortedByDescending { it.zIndex }.forEach { layer ->
            val isSelected = layer.id == selectedLayerId
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isSelected) Color(0xFF253146) else Color(0xFF1A2230),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectLayer(layer.id) }
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = layer.visible,
                            onCheckedChange = { onToggleVisibility(layer.id) }
                        )
                        Column {
                            Text(
                                text = layer.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "${layer.type.labelIt} · z:${layer.zIndex}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row {
                        TextButton(onClick = { onMoveLayer(layer.id, +1) }) { Text("Su") }
                        TextButton(onClick = { onMoveLayer(layer.id, -1) }) { Text("Giù") }
                    }
                }

                if (isSelected) {
                    Text(
                        text = "Opacità: ${(layer.opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Slider(
                        value = layer.opacity,
                        onValueChange = { onOpacityChange(layer.id, it) },
                        valueRange = 0f..1f
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BlendMode.entries.forEach { mode ->
                            TextButton(
                                onClick = { onBlendModeChange(layer.id, mode) }
                            ) {
                                Text(
                                    text = mode.labelIt,
                                    color = if (layer.blendMode == mode) Color(0xFF00C49F) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
