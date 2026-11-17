package io.github.tombolinero.ordo.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.VisibilityThreshold
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tombolinero.ordo.core.designsystem.component.BottomScrim
import io.github.tombolinero.ordo.core.designsystem.component.DotPillButton
import io.github.tombolinero.ordo.core.designsystem.component.FloatingNavBar
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.component.OrdoBackground
import io.github.tombolinero.ordo.core.designsystem.component.OrdoTopBar
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.ListSummary
import io.github.tombolinero.ordo.core.ui.ActionList
import io.github.tombolinero.ordo.core.ui.EmptyState
import io.github.tombolinero.ordo.core.ui.ListEditorSheet
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.SheetAction
import io.github.tombolinero.ordo.core.ui.UndoBar
import io.github.tombolinero.ordo.core.ui.UndoMessage
import io.github.tombolinero.ordo.core.ui.ordoViewModel
import io.github.tombolinero.ordo.core.ui.rememberUndoState

@Composable
fun HomeRoute(
    onOpenList: (ListId) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm = ordoViewModel { HomeViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    HomeScreen(state, vm, onOpenList, onOpenSearch, onOpenSettings)
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    vm: HomeViewModel?,
    onOpenList: (ListId) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val undo = rememberUndoState()
    var editorFor by remember { mutableStateOf<ListSummary?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<ListSummary?>(null) }

    OrdoBackground {
        Column(Modifier.fillMaxSize()) {
            OrdoTopBar(
                stringResource(R.string.home_title),
                navigation = { GlassIconButton(OrdoIcons.Search, stringResource(UiR.string.nav_search), onOpenSearch) },
            )
            when (state) {
                HomeUiState.Loading -> Unit
                is HomeUiState.Ready -> if (state.lists.isEmpty()) {
                    EmptyState(stringResource(R.string.home_empty_title), stringResource(R.string.home_empty_hint), Modifier.padding(bottom = 120.dp))
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(state.lists, key = { it.list.id.value }) { summary ->
                            ListCard(
                                summary,
                                onClick = { onOpenList(summary.list.id) },
                                onLongClick = { menuFor = summary },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = OrdoMotion.smooth(),
                                    placementSpec = OrdoMotion.smooth(IntOffset.VisibilityThreshold),
                                    fadeOutSpec = OrdoMotion.smooth(),
                                ),
                            )
                        }
                    }
                }
            }
        }
        BottomScrim()
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            UndoBar(undo, Modifier.padding(horizontal = 18.dp))
            FloatingNavBar(
                leading = { GlassIconButton(OrdoIcons.Lists, stringResource(UiR.string.nav_lists), {}, size = 56.dp, highlighted = false) },
                primary = { DotPillButton({ creating = true }, icon = OrdoIcons.Plus, contentDescription = stringResource(UiR.string.nav_new)) },
                trailing = { GlassIconButton(OrdoIcons.Settings, stringResource(UiR.string.nav_settings), onOpenSettings, size = 56.dp) },
            )
        }
    }

    if (creating) {
        ListEditorSheet(existing = null, onDismiss = { creating = false }) { title, description, color ->
            creating = false
            vm?.create(title, description, color, onOpenList)
        }
    }
    editorFor?.let { s ->
        ListEditorSheet(existing = s.list, onDismiss = { editorFor = null }) { title, description, color ->
            editorFor = null
            vm?.update(s.list.id, title, description, color)
        }
    }
    menuFor?.let { s ->
        val copySuffix = stringResource(R.string.list_copy_suffix)
        val trashedText = stringResource(R.string.list_trashed, s.list.title)
        val duplicatedText = stringResource(R.string.list_duplicated, s.list.title)
        val undoLabel = stringResource(UiR.string.action_undo)
        OrdoSheet(onDismiss = { menuFor = null }) {
            ActionList(
                s.list.title,
                listOf(
                    SheetAction(OrdoIcons.Edit, stringResource(R.string.list_edit)) { menuFor = null; editorFor = s },
                    SheetAction(OrdoIcons.Pin, stringResource(if (s.list.pinned) R.string.list_unpin else R.string.list_pin)) {
                        menuFor = null; vm?.setPinned(s.list.id, !s.list.pinned)
                    },
                    SheetAction(OrdoIcons.Copy, stringResource(R.string.list_duplicate)) {
                        menuFor = null; vm?.duplicate(s.list.id, copySuffix); undo.show(UndoMessage(duplicatedText))
                    },
                    SheetAction(OrdoIcons.Trash, stringResource(UiR.string.action_delete), destructive = true) {
                        menuFor = null
                        vm?.trash(s.list.id)
                        undo.show(UndoMessage(trashedText, undoLabel) { vm?.restore(s.list.id) })
                    },
                ),
            )
        }
    }
}
