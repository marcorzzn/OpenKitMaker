// Implements: SCR-EXPORT / FUN-EXPORT-PNG / FUN-EXPORT-ZIP
package com.openkitmaker.android.ui.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openkitmaker.android.i18n.StringsIt

@Composable
fun ExportScreen(
    onBackToEditor: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val kit by viewModel.kit.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${StringsIt.NAV_EXPORT} — ${kit?.name ?: ""}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            OutlinedButton(onClick = onBackToEditor) {
                Text("Torna all'Editor")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            previewBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Anteprima 512x512",
                    modifier = Modifier
                        .size(220.dp)
                        .background(Color(0xFF1A2230), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Esporta Singola Texture PNG (2048×2048):",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.exportGarmentPngLossless(com.openkitmaker.android.domain.GarmentType.JERSEY) },
                        enabled = !isExporting
                    ) {
                        Text("👕 Maglia")
                    }
                    Button(
                        onClick = { viewModel.exportGarmentPngLossless(com.openkitmaker.android.domain.GarmentType.SHORTS) },
                        enabled = !isExporting
                    ) {
                        Text("🩳 Shorts")
                    }
                    Button(
                        onClick = { viewModel.exportGarmentPngLossless(com.openkitmaker.android.domain.GarmentType.SOCKS) },
                        enabled = !isExporting
                    ) {
                        Text("🧦 Calze")
                    }
                }

                Button(
                    onClick = viewModel::exportInteroperableZip,
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Esporta Archivio Full Kit (.ZIP)")
                }

                if (isExporting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                exportStatus?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF00C49F)
                    )
                }
            }
        }
    }
}
