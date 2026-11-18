package io.github.tombolinero.ordo.feature.duel

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.tombolinero.ordo.core.data.DataGraph
import io.github.tombolinero.ordo.core.model.Duel
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

/** The duel against a real (in-memory) database: answers in, stored ranking out. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DuelViewModelTest {
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

    @Test fun `answers place the new entry at the right spot`() = runBlocking<Unit> {
        val repo = graph.repository
        val list = repo.createList("Autos")
        val a = repo.addItem(list, "A", placeIndex = 0)
        val b = repo.addItem(list, "B", placeIndex = 1)
        val c = repo.addItem(list, "C", placeIndex = 2)
        val new = repo.addItem(list, "Neu")

        val vm = DuelViewModel(repo, list, new)
        until { vm.state.value.duel != null }
        // 3 places: first opponent is the middle one (B). New is better than B, worse than A.
        assertThat(vm.state.value.opponents.single().id).isEqualTo(b)
        vm.answer(Duel.Answer.NewIsBetter)
        assertThat(vm.state.value.opponents.single().id).isEqualTo(a)
        vm.answer(Duel.Answer.NewIsWorse)
        until { vm.state.value.finalPlace != null }

        assertThat(vm.state.value.finalPlace).isEqualTo(2)
        val ranking = repo.getRanking(list)
        assertThat(ranking.places.map { it.items.single() }).containsExactly(a, new, b, c).inOrder()
    }

    @Test fun `equally good makes a tie`() = runBlocking<Unit> {
        val repo = graph.repository
        val list = repo.createList("Namen")
        val a = repo.addItem(list, "Emma", placeIndex = 0)
        val new = repo.addItem(list, "Mia")
        val vm = DuelViewModel(repo, list, new)
        until { vm.state.value.duel != null }
        vm.answer(Duel.Answer.Equal)
        until { vm.state.value.finalPlace != null }
        assertThat(vm.state.value.finalIsTie).isTrue()
        assertThat(repo.getRanking(list).places.single().items).containsExactly(a, new)
    }

    @Test fun `first entry of an empty list is placed without questions`() = runBlocking<Unit> {
        val repo = graph.repository
        val list = repo.createList("Leer")
        val new = repo.addItem(list, "Einziger")
        val vm = DuelViewModel(repo, list, new)
        until { vm.state.value.finalPlace != null }
        assertThat(vm.state.value.finalPlace).isEqualTo(1)
        assertThat(vm.state.value.duel?.answered).isEqualTo(0)
    }
}
