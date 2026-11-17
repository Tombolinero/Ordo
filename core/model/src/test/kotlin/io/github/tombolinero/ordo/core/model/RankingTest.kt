package io.github.tombolinero.ordo.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class RankingTest {
    private val a = ItemId("a")
    private val b = ItemId("b")
    private val c = ItemId("c")
    private val d = ItemId("d")
    private val e = ItemId("e")

    private fun ranking(vararg places: List<ItemId>, unranked: List<ItemId> = emptyList()) =
        Ranking(places.map { Ranking.Place(it) }, unranked)

    @Test fun `places are counted densely after a tie`() {
        val r = ranking(listOf(a), listOf(b), listOf(c, d), listOf(e))
        assertThat(r.placeOf(c)).isEqualTo(3)
        assertThat(r.placeOf(d)).isEqualTo(3)
        assertThat(r.placeOf(e)).isEqualTo(4)
    }

    @Test fun `move place keeps ties together`() {
        val r = ranking(listOf(a), listOf(b, c), listOf(d)).movePlace(1, 0)
        assertThat(r.places.map { it.items }).containsExactly(listOf(b, c), listOf(a), listOf(d)).inOrder()
    }

    @Test fun `merge creates a tie on the target place`() {
        val r = ranking(listOf(a), listOf(b), listOf(c)).mergePlaces(source = 2, target = 1)
        assertThat(r.places.map { it.items }).containsExactly(listOf(a), listOf(b, c)).inOrder()
        assertThat(r.placeOf(c)).isEqualTo(2)
    }

    @Test fun `split gives every entry its own place in order`() {
        val r = ranking(listOf(a), listOf(b, c, d)).splitPlace(1)
        assertThat(r.places.map { it.items }).containsExactly(listOf(a), listOf(b), listOf(c), listOf(d)).inOrder()
    }

    @Test fun `detach moves the entry directly below its former tie`() {
        val r = ranking(listOf(a, b), listOf(c)).detachFromTie(a)
        assertThat(r.places.map { it.items }).containsExactly(listOf(b), listOf(a), listOf(c)).inOrder()
    }

    @Test fun `placing an inbox entry removes it from the inbox`() {
        val r = ranking(listOf(a), listOf(b), unranked = listOf(c)).place(c, 1)
        assertThat(r.unranked).isEmpty()
        assertThat(r.placeOf(c)).isEqualTo(2)
        assertThat(r.placeOf(b)).isEqualTo(3)
    }

    @Test fun `placing as tie joins the existing place`() {
        val r = ranking(listOf(a), listOf(b), unranked = listOf(c)).place(c, 0, asTie = true)
        assertThat(r.places.first().items).containsExactly(a, c).inOrder()
    }

    @Test fun `removing the last entry of a place closes the gap`() {
        val r = ranking(listOf(a), listOf(b), listOf(c)).remove(b)
        assertThat(r.placeOf(c)).isEqualTo(2)
    }

    @Test fun `unplace moves the entry to the end of the inbox`() {
        val r = ranking(listOf(a), listOf(b), unranked = listOf(c)).unplace(a)
        assertThat(r.placeOf(b)).isEqualTo(1)
        assertThat(r.unranked).containsExactly(c, a).inOrder()
    }

    @Test fun `positions round trip, including ties and inbox`() {
        val r = ranking(listOf(a), listOf(b, c), unranked = listOf(d, e))
        assertThat(Ranking.fromPositions(r.toPositions().shuffled())).isEqualTo(r)
    }

    @Test fun `gaps in stored place numbers are closed`() {
        val r = Ranking.fromPositions(listOf(Ranking.Position(a, 0, 0), Ranking.Position(b, 7, 0)))
        assertThat(r.placeOf(b)).isEqualTo(2)
    }

    @Test fun `duplicates are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ranking(listOf(a), unranked = listOf(a)) }
    }
}
