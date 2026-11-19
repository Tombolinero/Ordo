package io.github.tombolinero.ordo.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RematchTest {
    private fun ids(n: Int) = (1..n).map { ItemId("e$it") }

    /** A user whose real opinion is [truth] (best first); equal "tier" values are ties. */
    private fun play(start: List<ItemId>, tier: (ItemId) -> Int): Rematch {
        var r = Rematch.start(start)
        while (!r.isFinished) {
            val me = tier(r.current!!)
            val them = tier(r.sorted.places[r.opponent!!].items.first())
            r = r.answer(
                when {
                    me < them -> Duel.Answer.NewIsBetter
                    me > them -> Duel.Answer.NewIsWorse
                    else -> Duel.Answer.Equal
                },
            )
        }
        return r
    }

    @Test fun `any starting order ends in the user's true order`() {
        val truth = ids(17)
        val rank = truth.withIndex().associate { it.value to it.index }
        for (seed in 0 until 30) {
            val start = truth.shuffled(kotlin.random.Random(seed))
            val r = play(start) { rank.getValue(it) }
            assertThat(r.result!!.places.map { it.items.single() }).containsExactlyElementsIn(truth).inOrder()
            assertThat(r.answered).isAtMost(Rematch.maxQuestions(17))
        }
        assertThat(Rematch.maxQuestions(17)).isEqualTo(54)
    }

    @Test fun `equally good answers create shared places`() {
        val items = ids(6)
        val tier = mapOf(items[0] to 1, items[1] to 2, items[2] to 2, items[3] to 3, items[4] to 1, items[5] to 3)
        val r = play(items) { tier.getValue(it) }
        val places = r.result!!.places.map { it.items.toSet() }
        assertThat(places).containsExactly(setOf(items[0], items[4]), setOf(items[1], items[2]), setOf(items[3], items[5])).inOrder()
    }

    @Test fun `undo goes back exactly one answer`() {
        val r0 = Rematch.start(ids(5))
        val r1 = r0.answer(Duel.Answer.NewIsBetter)
        val r2 = r1.answer(Duel.Answer.NewIsWorse)
        assertThat(r2.undo()).isEqualTo(r1)
        assertThat(r2.undo().undo()).isEqualTo(r0)
        assertThat(r0.canUndo).isFalse()
    }

    @Test fun `tiny lists need no or one question`() {
        assertThat(Rematch.start(emptyList()).isFinished).isTrue()
        val one = Rematch.start(ids(1))
        assertThat(one.isFinished).isTrue()
        assertThat(one.result!!.places).hasSize(1)
        val two = Rematch.start(ids(2))
        assertThat(two.isFinished).isFalse()
        assertThat(two.answer(Duel.Answer.NewIsBetter).isFinished).isTrue()
    }

    @Test fun `progress counts placed entries`() {
        var r = Rematch.start(ids(4))
        assertThat(r.placedCount).isEqualTo(1)
        while (!r.isFinished) r = r.answer(Duel.Answer.NewIsWorse)
        assertThat(r.placedCount).isEqualTo(4)
    }
}
