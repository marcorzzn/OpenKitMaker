// Implements: SCR-SAVED / FUN-SAVE-LOCAL
package com.openkitmaker.android.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openkitmaker.android.data.repository.KitRepository
import com.openkitmaker.android.domain.Kit
import com.openkitmaker.android.i18n.StringsIt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedKitsViewModel @Inject constructor(
    private val kitRepository: KitRepository
) : ViewModel() {

    val savedKits: StateFlow<List<Kit>> = kitRepository.observeAllKits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun renameKit(kitId: String, newName: String) {
        viewModelScope.launch { kitRepository.renameKit(kitId, newName) }
    }

    fun duplicateKit(kitId: String) {
        viewModelScope.launch { kitRepository.duplicateKit(kitId) }
    }

    fun deleteKit(kitId: String) {
        viewModelScope.launch { kitRepository.deleteKit(kitId) }
    }
}

@Composable
fun SavedKitsScreen(
    onOpenKit: (String) -> Unit,
    onExportKit: (String) -> Unit,
    viewModel: SavedKitsViewModel = hiltViewModel()
) {
    val kits by viewModel.savedKits.collectAsState()
    var kitToDelete by remember { mutableStateOf<Kit?>(null) }
    var kitToRename by remember { mutableStateOf<Kit?>(null) }
    var renameBuffer by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = StringsIt.NAV_SAVED,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(kits, key = { it.id }) { kit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A2230), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = kit.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${kit.category.labelIt} · Salvato: ${kit.updatedAt.take(16)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenKit(kit.id) }) { Text("Apri") }
                        OutlinedButton(onClick = {
                            kitToRename = kit
                            renameBuffer = kit.name
                        }) { Text("Rinomina") }
                        OutlinedButton(onClick = { viewModel.duplicateKit(kit.id) }) { Text("Duplica") }
                        OutlinedButton(onClick = { onExportKit(kit.id) }) { Text("Esporta") }
                        TextButton(onClick = { kitToDelete = kit }) {
                            Text("Elimina", color = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }

    kitToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { kitToDelete = null },
            title = { Text("Conferma eliminazione") },
            text = { Text("Eliminare definitivamente il kit '${target.name}' da Room SQLite?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteKit(target.id)
                    kitToDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { kitToDelete = null }) { Text("Annulla") }
            }
        )
    }

    kitToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { kitToRename = null },
            title = { Text("Rinomina Kit") },
            text = {
                OutlinedTextField(
                    value = renameBuffer,
                    onValueChange = { renameBuffer = it },
                    singleLine = true,
                    label = { Text("Nuovo nome") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameKit(target.id, renameBuffer)
                    kitToRename = null
                }) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { kitToRename = null }) { Text("Annulla") }
            }
        )
    }
}
