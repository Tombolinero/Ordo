package io.github.tombolinero.ordo.feature.ranking

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tombolinero.ordo.core.data.CropRect
import io.github.tombolinero.ordo.core.data.DataGraph
import io.github.tombolinero.ordo.core.model.ImageId
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankedList
import io.github.tombolinero.ordo.core.model.Ranking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankingUiState(
    val loading: Boolean = true,
    val list: RankedList? = null,
    val sorting: Boolean = false,
)

/** Result of an edit, for the undo bar. */
sealed interface RankingChange {
    val previous: Ranking

    data class Moved(override val previous: Ranking, val place: Int) : RankingChange
    data class Tied(override val previous: Ranking, val place: Int) : RankingChange
    data class Detached(override val previous: Ranking) : RankingChange
}

class RankingViewModel(private val graph: DataGraph, private val listId: ListId) : ViewModel() {
    private val repository = graph.repository
    private val sorting = MutableStateFlow(false)

    /** Applied immediately so a drop never waits for the database round trip. */
    private val optimistic = MutableStateFlow<Ranking?>(null)

    val state: StateFlow<RankingUiState> =
        combine(repository.observeRankedList(listId), sorting, optimistic) { list, sort, override ->
            val shown = if (list != null && override != null) {
                if (override == list.ranking) optimistic.value = null
                list.copy(ranking = override)
            } else {
                list
            }
            RankingUiState(loading = false, list = shown, sorting = sort)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingUiState())

    fun setSorting(on: Boolean) = sorting.update { on }

    private fun current(): Ranking? = state.value.list?.ranking

    private fun commit(next: Ranking) {
        optimistic.value = next
        viewModelScope.launch {
            runCatching { repository.saveRanking(listId, next) }.onFailure { optimistic.value = null }
        }
    }

    fun movePlace(from: Int, to: Int): RankingChange? {
        val r = current() ?: return null
        if (from == to) return null
        commit(r.movePlace(from, to))
        return RankingChange.Moved(r, to + 1)
    }

    fun mergePlaces(source: Int, target: Int): RankingChange? {
        val r = current() ?: return null
        if (source == target) return null
        val merged = r.mergePlaces(source, target)
        commit(merged)
        val place = merged.placeOf(r.places[target].items.first()) ?: (target + 1)
        return RankingChange.Tied(r, place)
    }

    fun detach(item: ItemId): RankingChange? {
        val r = current() ?: return null
        commit(r.detachFromTie(item))
        return RankingChange.Detached(r)
    }

    fun undo(change: RankingChange) {
        val r = current() ?: return
        // Only restore when the same entries are still there (nothing added or deleted since).
        if (r.toPositions().map { it.item }.toSet() == change.previous.toPositions().map { it.item }.toSet()) commit(change.previous)
    }

    fun moveToInbox(item: ItemId) {
        val r = current() ?: return
        commit(r.unplace(item))
    }

    fun trash(item: ItemId) = viewModelScope.launch { repository.moveItemToTrash(item) }

    fun restore(item: ItemId) = viewModelScope.launch { repository.restoreItem(item) }

    fun updateList(title: String, description: String, color: ListColor) =
        viewModelScope.launch { repository.updateList(listId, title, description, color) }

    /**
     * Saves a new entry. With [placeNow] it is placed right away when the list is still empty;
     * otherwise [onNeedsDuel] is called so the duel can find its place.
     */
    fun addItem(
        context: Context,
        title: String,
        note: String,
        imageUri: Uri?,
        crop: CropRect?,
        placeNow: Boolean,
        onNeedsDuel: (ItemId) -> Unit,
    ) = viewModelScope.launch {
        val image: ImageId? = imageUri?.let { runCatching { graph.images.import(context, it, crop ?: CropRect.Full) }.getOrNull() }
        val empty = current()?.places.isNullOrEmpty()
        val id = repository.addItem(listId, title, note, image, placeIndex = if (placeNow && empty) 0 else null)
        if (placeNow && !empty) onNeedsDuel(id)
    }
}
