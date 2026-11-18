package io.github.tombolinero.ordo

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.tombolinero.ordo.core.ui.OrdoAppGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.feature.duel.R as DuelR
import io.github.tombolinero.ordo.feature.ranking.R as RankingR

/**
 * End-to-end on a real Android system: drives the app through the screen like a person
 * would and checks the stored result. In CI the screen is recorded while this runs
 * (pass `-e ordoDemo true` to slow it down so the recording is watchable).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class OrdoFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val demo = InstrumentationRegistry.getArguments().getString("ordoDemo") == "true"
    private fun s(id: Int) = compose.activity.getString(id)
    private fun pause(ms: Long = 900) {
        if (demo) Thread.sleep(ms)
        compose.waitForIdle()
    }

    private fun tapDescription(id: Int) {
        compose.waitUntilAtLeastOneExists(hasContentDescription(s(id)), 10_000)
        compose.onAllNodes(hasContentDescription(s(id))).onFirst().performClick()
        pause()
    }

    private fun tapText(text: String) {
        compose.waitUntilAtLeastOneExists(hasText(text), 10_000)
        compose.onAllNodesWithText(text).onFirst().performClick()
        pause()
    }

    private fun type(text: String) {
        compose.waitUntilAtLeastOneExists(hasSetTextAction(), 10_000)
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput(text)
        pause(500)
    }

    @Test
    fun createListAddEntriesAndPlaceThem() {
        val title = "Beste Autos ${System.currentTimeMillis() % 10_000}"
        pause(1_500)

        // New list
        tapDescription(UiR.string.nav_new)
        type(title)
        tapText(s(UiR.string.editor_create))
        compose.waitUntilAtLeastOneExists(hasText(title), 10_000)

        // Three entries, placed later
        for (name in listOf("Koenigsegg Jesko", "Porsche Taycan", "Hyundai Ioniq 5")) {
            tapDescription(UiR.string.nav_new)
            type(name)
            tapText(s(RankingR.string.save_inbox))
            compose.waitUntilAtLeastOneExists(hasText(name), 10_000)
        }

        // 1st: empty ranking → lands on place 1 right away
        tapText(s(RankingR.string.inbox_place))
        tapText(s(DuelR.string.duel_done))
        // 2nd: one question; the new entry is better
        tapText(s(RankingR.string.inbox_place))
        compose.waitUntilAtLeastOneExists(hasText(s(DuelR.string.duel_question)), 10_000)
        tapText("Porsche Taycan")
        tapText(s(DuelR.string.duel_done))
        // 3rd: "equally good" against the first opponent → shares a place
        tapText(s(RankingR.string.inbox_place))
        compose.waitUntilAtLeastOneExists(hasText(s(DuelR.string.duel_question)), 10_000)
        tapText(s(DuelR.string.duel_equal))
        tapText(s(DuelR.string.duel_done))
        pause(1_500)

        // Sort mode on and off
        tapDescription(RankingR.string.sort)
        pause(1_200)
        tapText(s(UiR.string.action_done))
        pause(1_200)

        // Open the detail of number one and come back
        tapText("Porsche Taycan")
        pause(2_000)
        tapDescription(UiR.string.action_back)
        pause(1_200)

        val graph = (compose.activity.application as OrdoAppGraph).data
        runBlocking {
            val summary = graph.repository.observeSummaries().first().first { it.list.title == title }
            val ranked = graph.repository.observeRankedList(summary.list.id).first()!!
            val titles = ranked.ranking.places.map { p -> p.items.map { ranked.item(it).title }.toSet() }
            assertThat(titles).containsExactly(setOf("Porsche Taycan"), setOf("Koenigsegg Jesko", "Hyundai Ioniq 5")).inOrder()
            assertThat(ranked.ranking.unranked).isEmpty()
        }
    }
}
