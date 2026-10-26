// Implements: SCR-HOME / FUN-SAVE-LOCAL / FUN-OFFLINE-MODE
package com.openkitmaker.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openkitmaker.android.data.repository.KitRepository
import com.openkitmaker.android.domain.Kit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val kitRepository: KitRepository
) : ViewModel() {

    val recentKits: StateFlow<List<Kit>> = kitRepository.observeRecentKits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            kitRepository.ensureSeeded()
        }
    }
}
