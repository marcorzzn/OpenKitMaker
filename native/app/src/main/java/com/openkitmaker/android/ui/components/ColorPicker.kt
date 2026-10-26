// Implements: SCR-EDITOR / FUN-EDIT-COLOR
package com.openkitmaker.android.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Selettore colore HSV reattivo con supporto codice HEX (#RRGGBB / #RRGGBBAA)
 * e storico degli ultimi 8 colori utilizzati (`FUN-EDIT-COLOR`).
 */
@Composable
fun ColorPicker(
    label: String,
    currentHex: String,
    recentColors: List<String>,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hsv = remember(currentHex) {
        val out = FloatArray(3)
        val parsed = runCatching { AndroidColor.parseColor(currentHex) }.getOrDefault(AndroidColor.BLUE)
        AndroidColor.colorToHSV(parsed, out)
        out
    }

    var hue by remember(currentHex) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(currentHex) { mutableFloatStateOf(hsv[1]) }
    var value by remember(currentHex) { mutableFloatStateOf(hsv[2]) }
    var hexInput by remember(currentHex) { mutableStateOf(currentHex) }

    fun emitHsv() {
        val colorInt = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        val formatted = String.format("#%06X", 0xFFFFFF and colorInt)
        hexInput = formatted
        onColorChanged(formatted)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A2230), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(text = currentHex, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
        }

        OutlinedTextField(
            value = hexInput,
            onValueChange = { candidate ->
                hexInput = candidate
                if (candidate.matches(Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$"))) {
                    onColorChanged(candidate.uppercase())
                }
            },
            label = { Text("HEX (#RRGGBB)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Tonalità (H): ${hue.toInt()}°", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        Slider(
            value = hue,
            onValueChange = { hue = it; emitHsv() },
            valueRange = 0f..360f
        )

        Text("Saturazione (S): ${(saturation * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        Slider(
            value = saturation,
            onValueChange = { saturation = it; emitHsv() },
            valueRange = 0f..1f
        )

        Text("Luminosità (V): ${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        Slider(
            value = value,
            onValueChange = { value = it; emitHsv() },
            valueRange = 0f..1f
        )

        Text("Ultimi 8 colori recenti", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            recentColors.take(8).forEach { swatchHex ->
                val swatchColor = runCatching { Color(AndroidColor.parseColor(swatchHex)) }
                    .getOrDefault(Color(0xFF0F52BA))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(swatchColor, RoundedCornerShape(4.dp))
                        .border(
                            width = if (swatchHex.equals(currentHex, ignoreCase = true)) 2.dp else 1.dp,
                            color = if (swatchHex.equals(currentHex, ignoreCase = true)) Color.White else Color(0xFF2E3D56),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onColorChanged(swatchHex) }
                )
            }
        }
    }
}
