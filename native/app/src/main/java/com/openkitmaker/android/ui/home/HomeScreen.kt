// Implements: SCR-HOME / FUN-SAVE-LOCAL / FUN-OFFLINE-MODE
package com.openkitmaker.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openkitmaker.android.i18n.StringsIt

@Composable
fun HomeScreen(
    onOpenCatalog: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenKitInEditor: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentKits by viewModel.recentKits.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = StringsIt.APP_NAME,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${StringsIt.APP_OFFLINE_BADGE} · Google Filament Vulkan/GLES3 PBR",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onOpenCatalog) {
                    Text("Nuovo Kit da Template")
                }
                OutlinedButton(onClick = onOpenSaved) {
                    Text("Tutti i miei kit")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ultime 3 Bozze Modificate (Room SQLite)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        items(recentKits, key = { it.id }) { kit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A2230), RoundedCornerShape(8.dp))
                    .clickable { onOpenKitInEditor(kit.id) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = kit.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${kit.category.labelIt} · Aggiornato: ${kit.updatedAt.take(16)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                Text(
                    text = "Apri Editor",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF00C49F)
                )
            }
        }
    }
}
