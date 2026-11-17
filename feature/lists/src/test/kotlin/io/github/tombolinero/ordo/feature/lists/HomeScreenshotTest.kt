package io.github.tombolinero.ordo.feature.lists

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.ListSummary
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w393dp-h852dp-xxhdpi")
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun summary(title: String, color: ListColor, count: Int, ties: Int = 0, unranked: Int = 0, top: String? = null, pinned: Boolean = false) =
        ListSummary(
            list = RankList(ListId(title), title, color = color, pinned = pinned, createdAt = 0, updatedAt = 0),
            itemCount = count, tieCount = ties, unrankedCount = unranked,
            topItem = top?.let { RankItem(ItemId(it), ListId(title), it, createdAt = 0, updatedAt = 0) },
            previewImages = emptyList(),
        )

    private val sample = listOf(
        summary("Beste Autos", ListColor.Magenta, 8, ties = 1, unranked = 1, top = "Koenigsegg Jesko", pinned = true),
        summary("Babynamen", ListColor.Violet, 28, ties = 2, top = "Emma"),
        summary("Filme 2026", ListColor.Graphite, 41, top = "Dune: Teil Drei"),
        summary("Urlaubsziele", ListColor.Petrol, 9, unranked = 3, top = "Lofoten"),
        summary("Pizzerien", ListColor.Ruby, 12, top = "Da Michele"),
    )

    @Test fun home() {
        compose.setContent { OrdoTheme { HomeScreen(HomeUiState.Ready(sample), null, {}, {}, {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/home.png")
    }

    @Test fun homeEmpty() {
        compose.setContent { OrdoTheme { HomeScreen(HomeUiState.Ready(emptyList()), null, {}, {}, {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/home_empty.png")
    }
}
