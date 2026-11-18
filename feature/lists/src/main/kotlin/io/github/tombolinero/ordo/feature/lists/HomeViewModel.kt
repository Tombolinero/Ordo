package io.github.tombolinero.ordo.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tombolinero.ordo.core.data.OrdoRepository
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.ListSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val lists: List<ListSummary>) : HomeUiState
}

class HomeViewModel(private val repository: OrdoRepository) : ViewModel() {

    val state: StateFlow<HomeUiState> = repository.observeSummaries()
        .map<List<ListSummary>, HomeUiState> { HomeUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun create(title: String, description: String, color: ListColor, onCreated: (ListId) -> Unit) {
        viewModelScope.launch { onCreated(repository.createList(title, description, color)) }
    }

    fun update(id: ListId, title: String, description: String, color: ListColor) {
        viewModelScope.launch { repository.updateList(id, title, description, color) }
    }

    fun setPinned(id: ListId, pinned: Boolean) {
        viewModelScope.launch { repository.setPinned(id, pinned) }
    }

    fun duplicate(id: ListId, suffix: String) {
        viewModelScope.launch { repository.duplicateList(id, suffix) }
    }

    fun trash(id: ListId) {
        viewModelScope.launch { repository.moveListToTrash(id) }
    }

    fun restore(id: ListId) {
        viewModelScope.launch { repository.restoreList(id) }
    }
}
