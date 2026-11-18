package io.github.tombolinero.ordo.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.ln

class DuelTest {

    /** Simulates a user whose true opinion is [trueIndex] (insert before place trueIndex). */
    private fun play(places: Int, trueIndex: Int): Duel {
        var duel = Duel(places)
        while (!duel.isFinished) {
            val o = duel.opponent!!
            duel = duel.answer(if (trueIndex <= o) Duel.Answer.NewIsBetter else Duel.Answer.NewIsWorse)
        }
        return duel
    }

    @Test fun `finds every possible position`() {
        for (n in 0..40) for (target in 0..n) {
            assertThat(play(n, target).result).isEqualTo(Duel.Result.NewPlace(target))
        }
    }

    @Test fun `needs at most log2(n+1) questions`() {
        for (n in listOf(1, 7, 100, 1000)) {
            val bound = ceil(ln(n + 1.0) / ln(2.0)).toInt()
            for (target in 0..n step maxOf(1, n / 50)) {
                assertThat(play(n, target).answered).isAtMost(bound)
            }
        }
        assertThat(ceil(ln(101.0) / ln(2.0)).toInt()).isEqualTo(7)
    }

    @Test fun `equal ends the duel with a tie`() {
        val d = Duel(10).answer(Duel.Answer.Equal)
        assertThat(d.result).isEqualTo(Duel.Result.Tie(5))
        assertThat(d.isFinished).isTrue()
    }

    @Test fun `undo restores the previous question`() {
        val start = Duel(10)
        val after = start.answer(Duel.Answer.NewIsBetter)
        assertThat(after.opponent).isNotEqualTo(start.opponent)
        assertThat(after.undo().opponent).isEqualTo(start.opponent)
        assertThat(after.undo().answered).isEqualTo(0)
    }

    @Test fun `empty list places the first entry immediately`() {
        assertThat(Duel(0).result).isEqualTo(Duel.Result.NewPlace(0))
    }

    @Test fun `apply inserts into the ranking`() {
        val a = ItemId("a"); val b = ItemId("b"); val n = ItemId("n")
        val r = Ranking(listOf(Ranking.Place(listOf(a)), Ranking.Place(listOf(b))), listOf(n))
        val result = play(2, 1).applyTo(r, n)
        assertThat(result.placeOf(n)).isEqualTo(2)
        assertThat(result.unranked).isEmpty()
    }
}
