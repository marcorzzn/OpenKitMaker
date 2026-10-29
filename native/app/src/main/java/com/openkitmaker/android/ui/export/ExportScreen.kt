// Implements: SCR-EXPORT / FUN-EXPORT-PNG / FUN-EXPORT-ZIP, STEP 4
package com.openkitmaker.android.ui.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.openkitmaker.android.domain.ExportResolution
import com.openkitmaker.android.domain.GarmentType
import com.openkitmaker.android.i18n.StringsIt

@Composable
fun ExportScreen(
    onBackToEditor: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val kit by viewModel.kit.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val selectedResolution by viewModel.selectedResolution.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${StringsIt.NAV_EXPORT} — ${kit?.name ?: ""}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Tessuto: ${kit?.metadata?.fabricType?.labelIt ?: "Mesh"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00C49F)
                )
            }
            OutlinedButton(onClick = onBackToEditor) {
                Text("← Torna all'Editor", color = Color.White)
            }
        }

        // Preview & Resolution Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            previewBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Anteprima Full Kit 512x512",
                    modifier = Modifier
                        .size(180.dp)
                        .background(Color(0xFF1A2230), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF2E3D56), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Risoluzione Output 2D:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportResolution.entries.forEach { res ->
                        val isSelected = selectedResolution == res
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setResolution(res) },
                            label = { Text(res.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0F52BA),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1A2230),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }
        }

        // Hero Action: Social and WhatsApp Showcase Mockup (1080x1080 HD)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0F2B48), Color(0xFF143A3A))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(1.5.dp, Color(0xFF00C49F), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📸 Showcase Social & WhatsApp (1080×1080 HD)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Genera istantaneamente una cartolina quadrata HD del kit completo con sfondo studio e logo, perfetta per Instagram, WhatsApp o presentazioni alla squadra.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                Button(
                    onClick = { viewModel.exportGarmentPngLossless(GarmentType.JERSEY) },
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C49F)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Scarica 1080p HD", color = Color(0xFF0F141C), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 1: Texture 2D Singolo Capo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A2230), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF2E3D56), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Esporta Texture 2D (${selectedResolution.pixelSize}×${selectedResolution.pixelSize} Lossless PNG)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Campionamento ad alta risoluzione ideale per modding o rendering 3D.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportGarmentPngLossless(GarmentType.JERSEY) },
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F52BA))
                    ) {
                        Text("👕 Maglia")
                    }
                    Button(
                        onClick = { viewModel.exportGarmentPngLossless(GarmentType.SHORTS) },
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F52BA))
                    ) {
                        Text("🩳 Shorts")
                    }
                    Button(
                        onClick = { viewModel.exportGarmentPngLossless(GarmentType.SOCKS) },
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F52BA))
                    ) {
                        Text("🧦 Calze")
                    }
                }
            }
        }

        // Section 2: Universal Export (Full Kit ZIP, PBR Bundle, 3D GLB)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A2230), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF2E3D56), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Esportazione Universale & Asset 3D",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // 1. Interoperable Full Kit ZIP
                Button(
                    onClick = viewModel::exportInteroperableZip,
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C49F))
                ) {
                    Text("📦 Esporta Archivio Full Kit (.ZIP)", color = Color(0xFF0F141C), fontWeight = FontWeight.Bold)
                }

                // 2. PBR Materials Bundle ZIP
                Button(
                    onClick = viewModel::exportPbrBundle,
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Text("🎨 Esporta Bundle Materiali PBR (.ZIP)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // 3. Standalone 3D Model GLB
                Button(
                    onClick = viewModel::exportGlb3D,
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("🧊 Scarica Modello 3D Kit (.GLB)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Progress bar and status feedback
        if (isExporting) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF00C49F)
            )
        }

        exportStatus?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141B26), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF00C49F).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF00C49F)
                )
            }
        }
    }
}
