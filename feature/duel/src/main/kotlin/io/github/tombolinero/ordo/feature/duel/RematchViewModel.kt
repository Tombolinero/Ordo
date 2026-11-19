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
import io.github.tombolinero.ordo.core.model.Rematch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RematchUiState(
    val list: RankList? = null,
    val items: Map<ItemId, RankItem> = emptyMap(),
    /** The order before the rematch, to restore it afterwards if wanted. */
    val previous: Ranking = Ranking(),
    /** Null until the user taps "Start". */
    val rematch: Rematch? = null,
    val saved: Boolean = false,
    val restored: Boolean = false,
) {
    val entryCount: Int get() = previous.totalCount
}

/** Re-sorts a whole list with "A or B?" questions; stores only when finished. */
class RematchViewModel(private val repository: OrdoRepository, private val listId: ListId) : ViewModel() {
    private val _state = MutableStateFlow(RematchUiState())
    val state: StateFlow<RematchUiState> = _state

    init {
        viewModelScope.launch {
            val list = repository.observeRankedList(listId).first() ?: return@launch
            _state.value = RematchUiState(list.list, list.items, list.ranking)
        }
    }

    fun start() {
        val s = _state.value
        val order = s.previous.places.flatMap { it.items } + s.previous.unranked
        val r = Rematch.start(order)
        _state.value = s.copy(rematch = r)
        if (r.isFinished) save(r)
    }

    fun answer(answer: Duel.Answer) {
        val r = _state.value.rematch ?: return
        if (r.isFinished) return
        val next = r.answer(answer)
        _state.value = _state.value.copy(rematch = next)
        if (next.isFinished) save(next)
    }

    fun undo() {
        val s = _state.value
        val r = s.rematch ?: return
        if (s.saved) return
        _state.value = s.copy(rematch = r.undo())
    }

    fun restorePrevious() = viewModelScope.launch {
        val s = _state.value
        repository.saveRanking(listId, reconcile(s.previous, repository.getRanking(listId)))
        _state.value = s.copy(restored = true)
    }

    private fun save(r: Rematch) = viewModelScope.launch {
        val result = r.result ?: return@launch
        repository.saveRanking(listId, reconcile(result, repository.getRanking(listId)))
        _state.value = _state.value.copy(saved = true)
    }

    /**
     * Makes [wanted] fit the entries that exist right now: entries deleted meanwhile are
     * dropped, entries added meanwhile go to "not placed yet".
     */
    private fun reconcile(wanted: Ranking, current: Ranking): Ranking {
        val live = current.toPositions().map { it.item }.toSet()
        var r = wanted
        wanted.toPositions().map { it.item }.filter { it !in live }.forEach { r = r.remove(it) }
        live.filter { !r.contains(it) }.forEach { r = r.addUnranked(it) }
        return r
    }
}
