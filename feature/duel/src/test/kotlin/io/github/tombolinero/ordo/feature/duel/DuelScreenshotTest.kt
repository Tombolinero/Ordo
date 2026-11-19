package io.github.tombolinero.ordo.feature.duel

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.Duel
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankList
import io.github.tombolinero.ordo.core.model.Ranking
import io.github.tombolinero.ordo.core.model.Rematch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w393dp-h852dp-xxhdpi")
class DuelScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val l = ListId("cars")
    private fun item(id: String, title: String, note: String = "") = RankItem(ItemId(id), l, title, note, createdAt = 0, updatedAt = 0)
    private val items = (1..8).map { item("e$it", listOf("Koenigsegg Jesko", "Porsche Taycan", "Hyundai Ioniq 5", "Mercedes-Benz CLA", "BMW i4", "Polestar 2", "Kia EV6", "Tesla Model 3")[it - 1]) }
    private val newItem = item("new", "VW ID.7", "Viel Platz, sehr leise")
    private val base = Ranking(items.map { Ranking.Place(listOf(it.id)) })
    private val state = DuelUiState(
        list = RankList(l, "Beste Autos", color = ListColor.Ruby, createdAt = 0, updatedAt = 0),
        newItem = newItem,
        base = base,
        items = (items + newItem).associateBy { it.id },
        duel = Duel(8).answer(Duel.Answer.NewIsWorse),
    )

    @Test fun question() {
        compose.setContent { OrdoTheme { DuelScreen(state, {}, {}, {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/duel.png")
    }

    @Test fun result() {
        compose.setContent { OrdoTheme { DuelScreen(state.copy(finalPlace = 6), {}, {}, {}) } }
        compose.mainClock.advanceTimeBy(3_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/duel_result.png")
    }

    private val rematchState = RematchUiState(
        list = state.list,
        items = (items + newItem).associateBy { it.id },
        previous = Ranking(items.map { Ranking.Place(listOf(it.id)) }, unranked = listOf(newItem.id)),
    )

    @Test fun rematchIntro() {
        compose.setContent { OrdoTheme { RematchScreen(rematchState, {}, {}, {}, {}, {}) } }
        compose.mainClock.advanceTimeBy(2_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/rematch_intro.png")
    }

    @Test fun rematchQuestion() {
        var r = Rematch.start(items.map { it.id } + newItem.id)
        repeat(6) { r = r.answer(if (it % 2 == 0) Duel.Answer.NewIsWorse else Duel.Answer.NewIsBetter) }
        compose.setContent { OrdoTheme { RematchScreen(rematchState.copy(rematch = r), {}, {}, {}, {}, {}) } }
        compose.mainClock.advanceTimeBy(2_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/rematch_question.png")
    }

    @Test fun rematchDone() {
        var r = Rematch.start(items.map { it.id } + newItem.id)
        while (!r.isFinished) r = r.answer(Duel.Answer.NewIsBetter)
        compose.setContent { OrdoTheme { RematchScreen(rematchState.copy(rematch = r, saved = true), {}, {}, {}, {}, {}) } }
        compose.mainClock.advanceTimeBy(2_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/rematch_done.png")
    }
}
