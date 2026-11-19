package io.github.tombolinero.ordo.core.model

import kotlin.math.ceil
import kotlin.math.ln

/**
 * "Rematch": re-sorts a whole list from scratch with "A or B?" questions.
 *
 * Entries are inserted one by one into a new ranking (binary insertion, see [Duel]), so the
 * number of questions is at most Σ ⌈log2(k + 1)⌉ for k = 1 … n−1 (17 entries → at most 54).
 * "Equally good" puts both on the same place. Every answer can be undone. Nothing is stored
 * until [result] is taken, so cancelling keeps the old order untouched.
 */
data class Rematch(
    /** Entries still to be placed; the first one is the one being asked about. */
    val pending: List<ItemId>,
    /** The new order built so far. */
    val sorted: Ranking,
    val duel: Duel?,
    val total: Int,
    val answered: Int = 0,
    private val history: List<Rematch> = emptyList(),
) {
    val current: ItemId? get() = pending.firstOrNull()

    /** Place index in [sorted] to compare [current] with. */
    val opponent: Int? get() = duel?.opponent

    val isFinished: Boolean get() = pending.isEmpty()

    /** How many entries already have their new place (0 … total). */
    val placedCount: Int get() = total - pending.size

    val canUndo: Boolean get() = history.isNotEmpty()

    fun answer(answer: Duel.Answer): Rematch {
        val item = current ?: return this
        val d = duel ?: return this
        val next = d.answer(answer)
        val base = copy(history = history + this, answered = answered + 1)
        return if (next.isFinished) base.place(item, next) else base.copy(duel = next)
    }

    fun undo(): Rematch = history.lastOrNull() ?: this

    /** The finished order (all entries placed, nothing left in the inbox). */
    val result: Ranking? get() = if (isFinished) sorted else null

    private fun place(item: ItemId, finished: Duel): Rematch {
        val newSorted = finished.applyTo(sorted, item)
        return advance(copy(sorted = newSorted, pending = pending.drop(1), duel = null))
    }

    companion object {
        /** Starts with the entries in their current order (placed ones first, then the inbox). */
        fun start(entries: List<ItemId>): Rematch {
            require(entries.size == entries.toSet().size) { "Duplicate entries" }
            return advance(Rematch(pending = entries, sorted = Ranking(), duel = null, total = entries.size))
        }

        /** The first entry needs no question; afterwards each entry gets a fresh duel. */
        private fun advance(state: Rematch): Rematch {
            var s = state
            while (s.duel == null && s.pending.isNotEmpty()) {
                val d = Duel(s.sorted.places.size)
                s = if (d.isFinished) {
                    s.copy(sorted = d.applyTo(s.sorted, s.pending.first()), pending = s.pending.drop(1))
                } else {
                    s.copy(duel = d)
                }
            }
            return s
        }

        /** Upper bound of questions for [n] entries (shown before starting). */
        fun maxQuestions(n: Int): Int = (1 until n).sumOf { k -> ceil(ln(k + 1.0) / ln(2.0)).toInt() }
    }
}
