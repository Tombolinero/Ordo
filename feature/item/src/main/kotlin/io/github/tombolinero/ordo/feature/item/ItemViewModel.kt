package io.github.tombolinero.ordo.feature.item

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tombolinero.ordo.core.data.CropRect
import io.github.tombolinero.ordo.core.data.DataGraph
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankedList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemUiState(
    val list: RankedList? = null,
    /** All entries in display order: placed ones by place, then "not placed yet". */
    val order: List<ItemId> = emptyList(),
)

class ItemViewModel(private val graph: DataGraph, listId: ListId) : ViewModel() {
    private val repository = graph.repository

    val state: StateFlow<ItemUiState> = repository.observeRankedList(listId).map { list ->
        ItemUiState(list, list?.let { l -> l.ranking.places.flatMap { it.items } + l.ranking.unranked }.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemUiState())

    fun saveNote(item: RankItem, note: String) {
        if (note.trim() == item.note) return
        viewModelScope.launch { repository.updateItem(item.id, item.title, note, item.image) }
    }

    fun update(context: Context, item: RankItem, title: String, note: String, newImage: Uri?, crop: CropRect?, removeImage: Boolean) =
        viewModelScope.launch {
            val image = when {
                newImage != null -> runCatching { graph.images.import(context, newImage, crop ?: CropRect.Full) }.getOrNull() ?: item.image
                removeImage -> null
                else -> item.image
            }
            repository.updateItem(item.id, title, note, image)
            if (image != item.image) repository.cleanupImages()
        }

    fun moveToInbox(item: ItemId) = viewModelScope.launch {
        val ranking = repository.getRanking(state.value.list?.list?.id ?: return@launch)
        state.value.list?.list?.id?.let { repository.saveRanking(it, ranking.unplace(item)) }
    }

    fun trash(item: ItemId) = viewModelScope.launch { repository.moveItemToTrash(item) }
}
