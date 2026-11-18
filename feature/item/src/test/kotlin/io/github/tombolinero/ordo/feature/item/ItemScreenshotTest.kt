package io.github.tombolinero.ordo.feature.item

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankList
import io.github.tombolinero.ordo.core.model.RankedList
import io.github.tombolinero.ordo.core.model.Ranking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w393dp-h852dp-xxhdpi")
class ItemScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val l = ListId("cars")
    private fun item(id: String, title: String, note: String = "") = RankItem(ItemId(id), l, title, note, createdAt = 0, updatedAt = 0)
    private val items = listOf(
        item("jesko", "Koenigsegg Jesko"),
        item("taycan", "Porsche Taycan", "Fährt sich am besten von allen. Platz 2, weil der Jesko einfach unerreichbar ist. Probefahrt war im März."),
        item("ioniq", "Hyundai Ioniq 5"), item("cla", "Mercedes-Benz CLA"), item("i4", "BMW i4"),
        item("polestar", "Polestar 2"), item("ev6", "Kia EV6"), item("id7", "VW ID.7"),
    ).associateBy { it.id }
    private val ranking = Ranking(
        listOf("jesko", "taycan", "ioniq", "cla", "i4", "polestar", "ev6").map { Ranking.Place(listOf(ItemId(it))) },
        listOf(ItemId("id7")),
    )
    private val state = ItemUiState(
        RankedList(RankList(l, "Beste Autos", createdAt = 0, updatedAt = 0), items, ranking),
        ranking.places.flatMap { it.items } + ranking.unranked,
    )

    @Test fun detail() {
        compose.setContent { OrdoTheme { ItemScreen(state, ItemId("taycan"), null, {}, {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/item.png")
    }

    @Test fun notPlaced() {
        compose.setContent { OrdoTheme { ItemScreen(state, ItemId("id7"), null, {}, {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/item_not_placed.png")
    }
}
