package io.github.tombolinero.ordo.feature.item

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun update(context: Context, item: RankItem, title: String, note: String, pictures: List<PictureDraft>, cover: Int) =
        viewModelScope.launch {
            val ids = pictures.map { p ->
                when (p) {
                    is PictureDraft.Stored -> p.id
                    is PictureDraft.Picked -> runCatching { graph.images.import(context, p.uri, p.crop) }.getOrNull()
                }
            }
            val coverId = ids.getOrNull(cover) ?: ids.firstNotNullOfOrNull { it }
            val images = ids.filterNotNull()
            repository.updateItem(item.id, title, note, coverId, images)
            if (images != item.images) repository.cleanupImages()
        }

    fun moveToInbox(item: ItemId) = viewModelScope.launch {
        val ranking = repository.getRanking(state.value.list?.list?.id ?: return@launch)
        state.value.list?.list?.id?.let { repository.saveRanking(it, ranking.unplace(item)) }
    }

    fun trash(item: ItemId) = viewModelScope.launch { repository.moveItemToTrash(item) }
}
