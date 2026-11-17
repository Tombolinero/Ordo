package io.github.tombolinero.ordo.feature.search

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankList
import io.github.tombolinero.ordo.core.model.SearchHit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w393dp-h852dp-xxhdpi")
class SearchScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun hit(list: String, title: String, place: Int?, note: String = "") = SearchHit(
        RankItem(ItemId(title), ListId(list), title, note, createdAt = 0, updatedAt = 0),
        RankList(ListId(list), list, createdAt = 0, updatedAt = 0),
        place,
    )

    @Test fun results() {
        val hits = listOf(
            hit("Beste Autos", "Porsche Taycan", 2),
            hit("Beste Autos", "Porsche 911", null),
            hit("Babynamen", "Emma", 1, note = "wie die Porsche-Fahrerin aus dem Film"),
        )
        compose.setContent { OrdoTheme { SearchScreen("porsche", {}, hits, {}, {}, autoFocus = false) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/search.png")
    }
}
