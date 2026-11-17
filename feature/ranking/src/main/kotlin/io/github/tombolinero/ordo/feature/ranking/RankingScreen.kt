package io.github.tombolinero.ordo.feature.ranking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tombolinero.ordo.core.designsystem.component.BottomScrim
import io.github.tombolinero.ordo.core.designsystem.component.DotPillButton
import io.github.tombolinero.ordo.core.designsystem.component.FloatingNavBar
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.component.OrdoBackground
import io.github.tombolinero.ordo.core.designsystem.component.OrdoTopBar
import io.github.tombolinero.ordo.core.designsystem.component.PillowStyle
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankedList
import io.github.tombolinero.ordo.core.ui.ActionList
import io.github.tombolinero.ordo.core.ui.EmptyState
import io.github.tombolinero.ordo.core.ui.ListEditorSheet
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.SheetAction
import io.github.tombolinero.ordo.core.ui.UndoBar
import io.github.tombolinero.ordo.core.ui.UndoMessage
import io.github.tombolinero.ordo.core.ui.UndoState
import io.github.tombolinero.ordo.core.ui.ordoViewModel
import io.github.tombolinero.ordo.core.ui.pillow
import io.github.tombolinero.ordo.core.ui.rememberUndoState

@Composable
fun RankingRoute(
    listId: ListId,
    onBack: () -> Unit,
    onOpenItem: (ItemId) -> Unit,
    onPlace: (ItemId) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm = ordoViewModel(key = listId.value) { RankingViewModel(it, listId) }
    val state by vm.state.collectAsStateWithLifecycle()
    RankingScreen(state, vm, onBack, onOpenItem, onPlace, onOpenSettings)
}

@Composable
internal fun RankingScreen(
    state: RankingUiState,
    vm: RankingViewModel?,
    onBack: () -> Unit,
    onOpenItem: (ItemId) -> Unit,
    onPlace: (ItemId) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var sharing by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf(false) }
    val undo = rememberUndoState()
    var adding by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var itemMenu by remember { mutableStateOf<RankItem?>(null) }
    val list = state.list

    OrdoBackground {
        Column(Modifier.fillMaxSize()) {
            OrdoTopBar(
                list?.list?.title.orEmpty(),
                navigation = { GlassIconButton(OrdoIcons.Back, stringResource(UiR.string.action_back), onBack) },
                action = {
                    GlassIconButton(
                        OrdoIcons.Sort,
                        stringResource(R.string.sort),
                        { vm?.setSorting(!state.sorting) },
                        highlighted = state.sorting,
                        enabled = (list?.ranking?.places?.size ?: 0) > 1,
                    )
                },
            )
            if (list != null) {
                MetaRow(list, state.sorting)
                AnimatedContent(
                    targetState = state.sorting,
                    transitionSpec = { fadeIn(OrdoMotion.smooth()) togetherWith fadeOut(OrdoMotion.smooth()) },
                    label = "mode",
                    modifier = Modifier.weight(1f),
                ) { sorting ->
                    if (list.items.isEmpty()) {
                        EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_hint), Modifier.padding(bottom = 120.dp))
                    } else if (sorting) {
                        SortableRanking(
                            ranking = list.ranking,
                            items = list.items,
                            listState = rememberLazyListState(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 150.dp),
                            onMove = { from, to -> vm?.movePlace(from, to)?.let { undo.announce(it, context, vm) } },
                            onMerge = { from, to -> vm?.mergePlaces(from, to)?.let { undo.announce(it, context, vm) } },
                            onDetach = { id -> vm?.detach(id)?.let { undo.announce(it, context, vm) } },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        RankingList(list, onOpen = { onOpenItem(it.id) }, onLongPress = { itemMenu = it }, onPlace = onPlace)
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
            AnimatedContent(
                targetState = state.sorting,
                transitionSpec = {
                    (slideInVertically(OrdoMotion.bouncy(IntOffset.VisibilityThreshold)) { it } + fadeIn(OrdoMotion.smooth())) togetherWith
                        (slideOutVertically(OrdoMotion.smooth(IntOffset.VisibilityThreshold)) { it } + fadeOut(OrdoMotion.smooth()))
                },
                label = "bottom",
            ) { sorting ->
                if (sorting) {
                    GlassPillButton(stringResource(UiR.string.action_done), { vm?.setSorting(false) }, icon = OrdoIcons.Check, accent = true)
                } else {
                    FloatingNavBar(
                        leading = { GlassIconButton(OrdoIcons.Lists, stringResource(UiR.string.nav_lists), onBack, size = 56.dp) },
                        primary = { DotPillButton({ adding = true }, icon = OrdoIcons.Plus, contentDescription = stringResource(UiR.string.nav_new)) },
                        trailing = { GlassIconButton(OrdoIcons.More, stringResource(UiR.string.action_more), { menuOpen = true }, size = 56.dp) },
                    )
                }
            }
        }
    }

    if (adding) {
        AddItemSheet(onDismiss = { adding = false }) { title, note, image, crop, placeNow ->
            adding = false
            vm?.addItem(context, title, note, image, crop, placeNow, onNeedsDuel = onPlace)
        }
    }
    if (menuOpen) {
        OrdoSheet(onDismiss = { menuOpen = false }) {
            ActionList(
                list?.list?.title,
                listOf(
                    SheetAction(OrdoIcons.Sort, stringResource(R.string.sort)) { menuOpen = false; vm?.setSorting(true) },
                    SheetAction(OrdoIcons.Edit, stringResource(R.string.menu_edit_list)) { menuOpen = false; editingList = true },
                    SheetAction(OrdoIcons.Share, stringResource(R.string.menu_share)) { menuOpen = false; sharing = true },
                    SheetAction(OrdoIcons.Settings, stringResource(UiR.string.nav_settings)) { menuOpen = false; onOpenSettings() },
                ),
            )
        }
    }
    if (sharing && list != null) ShareRankingSheet(list) { sharing = false }
    if (editingList && list != null) {
        ListEditorSheet(list.list, onDismiss = { editingList = false }) { title, description, color ->
            editingList = false
            vm?.updateList(title, description, color)
        }
    }
    itemMenu?.let { item ->
        val trashed = stringResource(R.string.item_trashed, item.title)
        val toInbox = stringResource(R.string.item_to_inbox, item.title)
        val undoLabel = stringResource(UiR.string.action_undo)
        val placed = list?.ranking?.placeOf(item.id) != null
        OrdoSheet(onDismiss = { itemMenu = null }) {
            ActionList(
                item.title,
                buildList {
                    add(SheetAction(OrdoIcons.ChevronRight, stringResource(R.string.item_open)) { itemMenu = null; onOpenItem(item.id) })
                    add(SheetAction(OrdoIcons.Duel, stringResource(R.string.item_replace)) { itemMenu = null; onPlace(item.id) })
                    if (placed) {
                        add(SheetAction(OrdoIcons.Inbox, stringResource(R.string.item_to_inbox_action)) {
                            itemMenu = null; vm?.moveToInbox(item.id); undo.show(UndoMessage(toInbox))
                        })
                    }
                    add(SheetAction(OrdoIcons.Trash, stringResource(UiR.string.action_delete), destructive = true) {
                        itemMenu = null
                        vm?.trash(item.id)
                        undo.show(UndoMessage(trashed, undoLabel) { vm?.restore(item.id) })
                    })
                },
            )
        }
    }
}

private fun UndoState.announce(change: RankingChange, context: android.content.Context, vm: RankingViewModel) {
    val text = when (change) {
        is RankingChange.Moved -> context.getString(R.string.moved, change.place)
        is RankingChange.Tied -> context.getString(R.string.tied, change.place)
        is RankingChange.Detached -> context.getString(R.string.detached)
    }
    show(UndoMessage(text, context.getString(UiR.string.action_undo)) { vm.undo(change) })
}

/** "8 Einträge" left, ties right: the "Energy Status / Cells temp" line of the reference. */
@Composable
private fun MetaRow(list: RankedList, sorting: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(sorting, label = "meta") { s ->
            Text(
                if (s) stringResource(R.string.sort_mode) else pluralStringResource(UiR.plurals.entries_count, list.items.size, list.items.size),
                style = OrdoTheme.type.sectionTitle,
                color = if (s) OrdoPalette.Yellow else OrdoTheme.colors.textPrimary,
            )
        }
        val ties = list.ranking.places.count { it.isTie }
        if (ties > 0 && !sorting) {
            Text(pluralStringResource(UiR.plurals.ties_count, ties, ties), style = OrdoTheme.type.meta, color = OrdoTheme.colors.textSecondary)
        }
    }
    AnimatedVisibility(sorting, enter = fadeIn(OrdoMotion.smooth()), exit = fadeOut(OrdoMotion.smooth())) {
        Text(
            stringResource(R.string.sort_hint),
            style = OrdoTheme.type.label,
            color = OrdoTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
        )
    }
}

/** Normal mode: podium for the top three, compact rows after, then the inbox. */
@Composable
private fun RankingList(
    list: RankedList,
    onOpen: (RankItem) -> Unit,
    onLongPress: (RankItem) -> Unit,
    onPlace: (ItemId) -> Unit,
) {
    val listStyle = list.list.color.pillow()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(list.ranking.places, key = { _, p -> "place-" + p.items.first().value }) { index, place ->
            val size = sizeForPlace(index)
            PlaceCard(
                number = index + 1,
                members = place.items.map(list::item),
                size = size,
                style = if (index == 0) listStyle else PillowStyle.Graphite,
                onOpen = onOpen,
                onLongPress = onLongPress,
                numberColor = if (index == 0 && listStyle == PillowStyle.Graphite) OrdoPalette.Yellow else androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.animateItem(
                    fadeInSpec = OrdoMotion.smooth(),
                    placementSpec = OrdoMotion.smooth(IntOffset.VisibilityThreshold),
                    fadeOutSpec = OrdoMotion.smooth(),
                ),
            )
        }
        inbox(list, onOpen, onLongPress, onPlace)
    }
}

private fun LazyListScope.inbox(list: RankedList, onOpen: (RankItem) -> Unit, onLongPress: (RankItem) -> Unit, onPlace: (ItemId) -> Unit) {
    if (list.ranking.unranked.isEmpty()) return
    item(key = "inbox-header") {
        Text(
            stringResource(R.string.inbox_title).uppercase() + "  ·  " + list.ranking.unranked.size,
            style = OrdoTheme.type.overline,
            color = OrdoTheme.colors.textTertiary,
            modifier = Modifier.padding(start = 8.dp, top = 22.dp, bottom = 4.dp).animateItem(),
        )
    }
    items(list.ranking.unranked, key = { "inbox-" + it.value }) { id ->
        val item = list.item(id)
        InboxRow(item, onOpen, onLongPress, onPlace, Modifier.animateItem(placementSpec = OrdoMotion.smooth(IntOffset.VisibilityThreshold)))
    }
}

@Composable
private fun InboxRow(item: RankItem, onOpen: (RankItem) -> Unit, onLongPress: (RankItem) -> Unit, onPlace: (ItemId) -> Unit, modifier: Modifier) {
    Box(modifier) {
        PlaceCardInbox(item, onOpen, onLongPress) {
            GlassPillButton(stringResource(R.string.inbox_place), { onPlace(item.id) }, icon = OrdoIcons.Duel, accent = true)
        }
    }
}
