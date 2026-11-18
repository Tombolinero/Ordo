package io.github.tombolinero.ordo.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankList
import io.github.tombolinero.ordo.core.model.TrashEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w393dp-h852dp-xxhdpi")
class SettingsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settings() {
        compose.setContent { OrdoTheme { SettingsScreen("1.0.0", 3, BackupState.Idle, {}, {}, {}, { _, _ -> }, {}, {}, {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/settings.png")
    }

    @Test fun trash() {
        val day = 86_400_000L
        val now = 100 * day
        val entries = listOf(
            TrashEntry.Item(RankItem(ItemId("a"), ListId("l"), "Tesla Model 3", createdAt = 0, updatedAt = 0, deletedAt = now - 2 * day), "Beste Autos"),
            TrashEntry.List(RankList(ListId("x"), "Pizzerien", createdAt = 0, updatedAt = 0, deletedAt = now - 12 * day), 12),
        )
        compose.setContent { OrdoTheme { TrashScreen(entries, now, {}, {}, {}, {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/trash.png")
    }
}
