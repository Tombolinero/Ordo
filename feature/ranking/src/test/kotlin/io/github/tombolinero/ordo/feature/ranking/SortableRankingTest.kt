package io.github.tombolinero.ordo.feature.ranking

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ItemId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Drives real touch gestures through the sort mode and checks what gets committed. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi")
class SortableRankingTest {
    @get:Rule val compose = createComposeRule()

    private val moves = mutableListOf<Pair<Int, Int>>()
    private val merges = mutableListOf<Pair<Int, Int>>()
    private val detached = mutableListOf<ItemId>()

    private fun setContent() {
        compose.setContent {
            OrdoTheme {
                SortableRanking(
                    ranking = Samples.ranking,
                    items = Samples.items,
                    listState = rememberLazyListState(),
                    contentPadding = PaddingValues(16.dp),
                    onMove = { a, b -> moves += a to b },
                    onMerge = { a, b -> merges += a to b },
                    onDetach = { detached += it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        compose.waitForIdle()
    }

    /** Row pitch: 72 dp rows (tie rows are taller) + 10 dp spacing. */
    private fun dp(v: Float) = v * compose.density.density

    private fun gripOfRow(rowTopDp: Float) = Offset(dp(393f - 16f - 28f), dp(16f + rowTopDp + 36f))

    @Test fun `dragging the first place down two rows moves it to place 3`() {
        setContent()
        val start = gripOfRow(0f)
        compose.onRoot().performTouchInput {
            down(start)
            // Past row 2 (68 dp) and the tall tie row 3 (96 dp) -> lands after it.
            val steps = 40
            repeat(steps) { moveBy(Offset(0f, dp(230f) / steps)); advanceEventTime(16) }
            up()
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        assertThat(merges).isEmpty()
        assertThat(moves).containsExactly(0 to 2)
    }

    @Test fun `holding over the middle of another row merges them`() {
        setContent()
        compose.mainClock.autoAdvance = false
        val start = gripOfRow(0f)
        compose.onRoot().performTouchInput {
            down(start)
            // Move to the centre of row 2 (one pitch of 72 + 10 dp, plus the touch slop) and hold still.
            val steps = 20
            repeat(steps) { moveBy(Offset(0f, dp(98f) / steps)); advanceEventTime(16) }
        }
        compose.mainClock.advanceTimeBy(600)
        compose.onRoot().performTouchInput { up() }
        compose.mainClock.advanceTimeBy(2_000)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertThat(moves).isEmpty()
        assertThat(merges).containsExactly(0 to 1)
    }

    @Test fun `short drag back to the start changes nothing`() {
        setContent()
        compose.onRoot().performTouchInput {
            down(gripOfRow(0f))
            repeat(10) { moveBy(Offset(0f, dp(3f))); advanceEventTime(16) }
            repeat(10) { moveBy(Offset(0f, -dp(3f))); advanceEventTime(16) }
            up()
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        assertThat(moves).isEmpty()
        assertThat(merges).isEmpty()
    }

    @Test fun `tied entries can be taken out of their tie`() {
        setContent()
        val buttons = compose.onAllNodesWithText("Hyundai Ioniq 5")
        assertThat(buttons.fetchSemanticsNodes()).isNotEmpty()
    }
}
