package io.github.tombolinero.ordo.feature.ranking

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ListColor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w393dp-h852dp-xxhdpi")
class RankingScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun shoot(name: String, state: RankingUiState) {
        compose.setContent { OrdoTheme { RankingScreen(state, null, {}, {}, {}, {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun podium() = shoot("ranking", RankingUiState(loading = false, list = Samples.list()))

    @Test fun podiumGraphite() = shoot("ranking_graphite", RankingUiState(loading = false, list = Samples.list(ListColor.Graphite)))

    @Test fun shareCard() {
        compose.setContent { OrdoTheme { ShareCard(Samples.list()) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/share_card.png")
    }

    @Test fun sortMode() = shoot("ranking_sort", RankingUiState(loading = false, list = Samples.list(), sorting = true))
}
