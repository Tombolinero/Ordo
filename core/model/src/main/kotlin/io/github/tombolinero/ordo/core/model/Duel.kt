package io.github.tombolinero.ordo.core.model

/**
 * "A oder B?": finds the place for a new entry by asking the user to compare it with
 * existing places, using binary search. With n places it needs at most ⌈log2(n + 1)⌉ answers
 * (100 places → 7 questions).
 *
 * The state is immutable; every answer returns a new state, and [undo] steps back.
 */
data class Duel(
    val placeCount: Int,
    private val low: Int = 0,
    private val high: Int = placeCount,
    private val tieWith: Int? = null,
    private val history: List<Duel> = emptyList(),
) {
    enum class Answer { NewIsBetter, NewIsWorse, Equal }

    sealed interface Result {
        /** New entry gets its own place at this index. */
        data class NewPlace(val index: Int) : Result

        /** New entry shares the existing place at this index. */
        data class Tie(val index: Int) : Result
    }

    val isFinished: Boolean get() = tieWith != null || low >= high

    /** Index of the place to compare the new entry with, or null when finished. */
    val opponent: Int? get() = if (isFinished) null else (low + high) / 2

    val answered: Int get() = history.size

    /** Upper bound of questions still to come (for the progress ring). */
    val remainingAtMost: Int
        get() = if (isFinished) 0 else 32 - Integer.numberOfLeadingZeros(high - low)

    val result: Result?
        get() = when {
            tieWith != null -> Result.Tie(tieWith)
            low >= high -> Result.NewPlace(low)
            else -> null
        }

    fun answer(answer: Answer): Duel {
        val mid = opponent ?: return this
        val next = when (answer) {
            Answer.NewIsBetter -> copy(high = mid)
            Answer.NewIsWorse -> copy(low = mid + 1)
            Answer.Equal -> copy(tieWith = mid)
        }
        return next.copy(history = history + this)
    }

    fun undo(): Duel = history.lastOrNull() ?: this

    /** Applies the result to [ranking]. */
    fun applyTo(ranking: Ranking, item: ItemId): Ranking = when (val r = result) {
        is Result.NewPlace -> ranking.place(item, r.index)
        is Result.Tie -> ranking.place(item, r.index, asTie = true)
        null -> error("Duel is not finished")
    }
}
