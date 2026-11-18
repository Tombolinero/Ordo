package io.github.tombolinero.ordo.feature.ranking

import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankList
import io.github.tombolinero.ordo.core.model.RankedList
import io.github.tombolinero.ordo.core.model.Ranking

/** Example data for screenshots (not real user data). */
object Samples {
    private val listId = ListId("cars")
    private fun item(id: String, title: String, note: String = "") = RankItem(ItemId(id), listId, title, note, createdAt = 0, updatedAt = 0)

    val items = listOf(
        item("jesko", "Koenigsegg Jesko", "Über 1.600 PS – ein Kunstwerk auf Rädern"),
        item("taycan", "Porsche Taycan", "Elektrisch und das beste Fahrwerk"),
        item("ioniq", "Hyundai Ioniq 5", "800-Volt-Laden, Retro-Design"),
        item("cla", "Mercedes-Benz CLA", "Schöne Linie"),
        item("i4", "BMW i4", "Fährt sich wie ein BMW"),
        item("polestar", "Polestar 2", "Minimalistisch innen"),
        item("ev6", "Kia EV6", "Unterschätzt"),
        item("model3", "Tesla Model 3"),
        item("id7", "VW ID.7"),
    ).associateBy { it.id }

    private fun p(vararg ids: String) = Ranking.Place(ids.map(::ItemId))

    val ranking = Ranking(
        places = listOf(p("jesko"), p("taycan"), p("ioniq", "cla"), p("i4"), p("polestar"), p("ev6")),
        unranked = listOf(ItemId("model3"), ItemId("id7")),
    )

    fun list(color: ListColor = ListColor.Magenta) = RankedList(
        RankList(listId, "Beste Autos", color = color, createdAt = 0, updatedAt = 0),
        items,
        ranking,
    )
}
