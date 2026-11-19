package io.github.tombolinero.ordo.feature.duel

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.tombolinero.ordo.core.data.DataGraph
import io.github.tombolinero.ordo.core.model.Duel
import io.github.tombolinero.ordo.core.model.ItemId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Rematch against a real (in-memory) database. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RematchViewModelTest {
    private lateinit var graph: DataGraph

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        graph = DataGraph(ApplicationProvider.getApplicationContext(), "test", 1, databaseName = null)
    }

    @After fun tearDown() {
        graph.close()
        Dispatchers.resetMain()
    }

    private suspend fun until(condition: () -> Boolean) = withTimeout(5_000) { while (!condition()) delay(10) }

    /** Answers like someone whose taste is exactly [wanted] (first = best). */
    private fun RematchViewModel.answerBy(wanted: List<ItemId>) {
        while (true) {
            val r = state.value.rematch ?: return
            if (r.isFinished) return
            val opp = r.sorted.places[r.opponent!!].items.first()
            answer(if (wanted.indexOf(r.current!!) < wanted.indexOf(opp)) Duel.Answer.NewIsBetter else Duel.Answer.NewIsWorse)
        }
    }

    @Test fun `rematch re-sorts the whole list including the inbox`() = runBlocking<Unit> {
        val repo = graph.repository
        val list = repo.createList("Autos")
        val a = repo.addItem(list, "A", placeIndex = 0)
        val b = repo.addItem(list, "B", placeIndex = 1)
        val c = repo.addItem(list, "C", placeIndex = 2)
        val d = repo.addItem(list, "D") // not placed yet
        val vm = RematchViewModel(repo, list)
        until { vm.state.value.list != null }
        assertThat(vm.state.value.entryCount).isEqualTo(4)

        vm.start()
        val wanted = listOf(d, c, a, b)
        vm.answerBy(wanted)
        until { vm.state.value.saved }
        val ranking = repo.getRanking(list)
        assertThat(ranking.places.map { it.items.single() }).containsExactlyElementsIn(wanted).inOrder()
        assertThat(ranking.unranked).isEmpty()
    }

    @Test fun `nothing is stored before the end and the old order can be restored`() = runBlocking<Unit> {
        val repo = graph.repository
        val list = repo.createList("Namen")
        val a = repo.addItem(list, "Emma", placeIndex = 0)
        val b = repo.addItem(list, "Mia", placeIndex = 1)
        val c = repo.addItem(list, "Lea", placeIndex = 2)
        val before = repo.getRanking(list)
        val vm = RematchViewModel(repo, list)
        until { vm.state.value.list != null }
        vm.start()
        vm.answer(Duel.Answer.NewIsBetter)
        assertThat(repo.getRanking(list)).isEqualTo(before)

        vm.answerBy(listOf(c, b, a))
        until { vm.state.value.saved }
        assertThat(repo.getRanking(list).places.first().items.single()).isEqualTo(c)

        vm.restorePrevious()
        until { vm.state.value.restored }
        assertThat(repo.getRanking(list)).isEqualTo(before)
    }

    @Test fun `entries added during the rematch are kept`() = runBlocking<Unit> {
        val repo = graph.repository
        val list = repo.createList("Filme")
        val a = repo.addItem(list, "A", placeIndex = 0)
        val b = repo.addItem(list, "B", placeIndex = 1)
        val vm = RematchViewModel(repo, list)
        until { vm.state.value.list != null }
        vm.start()
        val late = repo.addItem(list, "Neu")
        vm.answer(Duel.Answer.NewIsBetter)
        until { vm.state.value.saved }
        val ranking = repo.getRanking(list)
        assertThat(ranking.places.map { it.items.single() }).containsExactly(b, a).inOrder()
        assertThat(ranking.unranked).containsExactly(late)
    }
}
