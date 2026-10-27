// Implements: SCR-CATALOG / FUN-SELECT-TEMPLATE
package com.openkitmaker.android.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.openkitmaker.android.domain.KitCategory
import com.openkitmaker.android.domain.Template
import com.openkitmaker.android.i18n.StringsIt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val kitRepository: KitRepository
) : ViewModel() {

    val templates: StateFlow<List<Template>> = kitRepository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectAndCloneTemplate(template: Template, onCloned: (String) -> Unit) {
        viewModelScope.launch {
            val created = kitRepository.cloneKitFromTemplate(template)
            onCloned(created.id)
        }
    }
}

@Composable
fun CatalogScreen(
    onTemplateCloned: (String) -> Unit,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    var selectedCategory by remember { mutableStateOf<KitCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = templates.filter { tpl ->
        val matchesCategory = selectedCategory == null || tpl.category == selectedCategory
        val matchesQuery = searchQuery.isBlank() ||
            tpl.name.contains(searchQuery, ignoreCase = true) ||
            tpl.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = StringsIt.CATALOG_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Cerca template per nome o geometria...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("Tutti") }
            )
            KitCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.labelIt) }
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered, key = { it.id }) { tpl ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A2230), RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectAndCloneTemplate(tpl, onTemplateCloned) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${tpl.name} · ${tpl.category.labelIt}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (tpl.id.startsWith("tpl-fullkit")) {
                            Surface(
                                color = Color(0xFF1E3A8A),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "👕🩳🧦 Full Kit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF93C5FD),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = tpl.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Button(
                        onClick = { viewModel.selectAndCloneTemplate(tpl, onTemplateCloned) }
                    ) {
                        Text("Usa questo Template")
                    }
                }
            }
        }
    }
}
