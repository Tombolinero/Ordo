package io.github.tombolinero.ordo.feature.duel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tombolinero.ordo.core.data.OrdoRepository
import io.github.tombolinero.ordo.core.model.Duel
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankList
import io.github.tombolinero.ordo.core.model.Ranking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DuelUiState(
    val list: RankList? = null,
    val newItem: RankItem? = null,
    /** The ranking without the entry being placed. */
    val base: Ranking = Ranking(),
    val items: Map<ItemId, RankItem> = emptyMap(),
    val duel: Duel? = null,
    /** 1-based final place once saved. */
    val finalPlace: Int? = null,
    val finalIsTie: Boolean = false,
) {
    val opponents: List<RankItem>
        get() = duel?.opponent?.let { base.places[it].items.mapNotNull(items::get) }.orEmpty()
}

/** Places one entry by asking "A or B?" (binary search over the places, see core/model/Duel). */
class DuelViewModel(private val repository: OrdoRepository, private val listId: ListId, private val itemId: ItemId) : ViewModel() {
    private val _state = MutableStateFlow(DuelUiState())
    val state: StateFlow<DuelUiState> = _state

    init {
        viewModelScope.launch {
            val list = repository.observeRankedList(listId).first() ?: return@launch
            val base = list.ranking.remove(itemId)
            val duel = Duel(base.places.size)
            _state.value = DuelUiState(list.list, list.items[itemId], base, list.items, duel)
            if (duel.isFinished) finish(duel)
        }
    }

    fun answer(answer: Duel.Answer) {
        val duel = _state.value.duel ?: return
        if (duel.isFinished) return
        val next = duel.answer(answer)
        _state.value = _state.value.copy(duel = next)
        if (next.isFinished) finish(next)
    }

    fun undo() {
        val s = _state.value
        val duel = s.duel ?: return
        if (s.finalPlace != null) return
        _state.value = s.copy(duel = duel.undo())
    }

    private fun finish(duel: Duel) = viewModelScope.launch {
        // Apply to the latest stored order, in case something changed meanwhile.
        val fresh = repository.getRanking(listId)
        val result = duel.applyTo(fresh, itemId)
        repository.saveRanking(listId, result)
        _state.value = _state.value.copy(
            duel = duel,
            finalPlace = result.placeOf(itemId),
            finalIsTie = duel.result is Duel.Result.Tie,
        )
    }
}
