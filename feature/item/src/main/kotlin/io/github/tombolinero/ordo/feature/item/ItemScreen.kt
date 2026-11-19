package io.github.tombolinero.ordo.feature.item

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.component.OrdoBackground
import io.github.tombolinero.ordo.core.designsystem.component.OrdoTopBar
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.component.RankWheel
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankedList
import io.github.tombolinero.ordo.core.ui.ActionList
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.OrdoTextField
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.SharedKeys
import io.github.tombolinero.ordo.core.ui.SheetAction
import io.github.tombolinero.ordo.core.ui.ordoViewModel
import io.github.tombolinero.ordo.core.ui.sharedBounds
import kotlinx.coroutines.delay

@Composable
fun ItemRoute(listId: ListId, itemId: ItemId, onBack: () -> Unit, onPlace: (ItemId) -> Unit) {
    val vm = ordoViewModel(key = "item-${listId.value}") { ItemViewModel(it, listId) }
    val state by vm.state.collectAsStateWithLifecycle()
    ItemScreen(state, itemId, vm, onBack, onPlace)
}

/**
 * Detail of one entry (decision E6 B). The picture grows out of the list row (shared
 * element), below it the rank wheel of the reference: the ring is split into one segment per
 * place, the yellow mark is this entry, with the neighbouring places left and right of the
 * big number. Swipe sideways to walk through the ranking.
 */
@Composable
internal fun ItemScreen(state: ItemUiState, startItem: ItemId, vm: ItemViewModel?, onBack: () -> Unit, onPlace: (ItemId) -> Unit) {
    val list = state.list
    var menuFor by remember { mutableStateOf<RankItem?>(null) }
    var editing by remember { mutableStateOf<RankItem?>(null) }
    OrdoBackground {
        if (list == null || state.order.isEmpty()) return@OrdoBackground
        val startPage = state.order.indexOf(startItem).coerceAtLeast(0)
        val pager = rememberPagerState(initialPage = startPage) { state.order.size }
        val current = list.items[state.order.getOrNull(pager.currentPage)]
        Column(Modifier.fillMaxSize()) {
            OrdoTopBar(
                list.list.title,
                navigation = { GlassIconButton(OrdoIcons.Back, stringResource(UiR.string.action_back), onBack) },
                action = { GlassIconButton(OrdoIcons.More, stringResource(UiR.string.action_more), { menuFor = current }) },
            )
            HorizontalPager(pager, Modifier.weight(1f), beyondViewportPageCount = 1, key = { state.order[it].value }) { page ->
                val item = list.items[state.order[page]] ?: return@HorizontalPager
                // Only the visible page morphs with the list; neighbours stay put.
                ItemPage(list, item, vm, onPlace, shared = page == pager.currentPage)
            }
        }
    }
    menuFor?.let { item ->
        val placed = list?.ranking?.placeOf(item.id) != null
        OrdoSheet(onDismiss = { menuFor = null }) {
            ActionList(
                item.title,
                buildList {
                    add(SheetAction(OrdoIcons.Edit, stringResource(R.string.edit)) { menuFor = null; editing = item })
                    add(SheetAction(OrdoIcons.Duel, stringResource(R.string.replace)) { menuFor = null; onPlace(item.id) })
                    if (placed) add(SheetAction(OrdoIcons.Inbox, stringResource(R.string.to_inbox)) { menuFor = null; vm?.moveToInbox(item.id) })
                    add(SheetAction(OrdoIcons.Trash, stringResource(UiR.string.action_delete), destructive = true) {
                        menuFor = null; vm?.trash(item.id); onBack()
                    })
                },
            )
        }
    }
    editing?.let { item ->
        val context = LocalContext.current
        EditItemSheet(item, onDismiss = { editing = null }) { title, note, pictures, cover ->
            editing = null
            vm?.update(context, item, title, note, pictures, cover)
        }
    }
}

@Composable
private fun ItemPage(list: RankedList, item: RankItem, vm: ItemViewModel?, onPlace: (ItemId) -> Unit, shared: Boolean) {
    val placeIndex = list.ranking.indexOfPlace(item.id)
    val placed = placeIndex >= 0
    val placeCount = list.ranking.places.size
    var viewing by remember { mutableStateOf<Int?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ItemImage(
            item.image, item.title, 220.dp,
            Modifier.padding(top = 8.dp).then(if (shared) Modifier.sharedBounds(SharedKeys.itemImage(item.id.value)) else Modifier)
                .then(if (item.image != null) Modifier.clickable { viewing = item.images.indexOf(item.image).coerceAtLeast(0) } else Modifier),
            shape = SmoothCornerShape(44.dp),
        )
        if (item.images.size > 1) {
            PictureRow(item.images, item.image, item.title, onOpen = { viewing = it }, modifier = Modifier.padding(top = 14.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            item.title,
            Modifier.padding(horizontal = 28.dp).then(if (shared) Modifier.sharedBounds(SharedKeys.itemTitle(item.id.value)) else Modifier),
            style = OrdoTheme.type.display,
            color = OrdoTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        if (placed) {
            RankWheel(count = placeCount, position = placeIndex + 1, diameter = 270.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.padding(top = 18.dp)) {
                            if (placeIndex > 0) DotMatrixText(placeIndex.toString().padStart(2, '0'), 14.dp, color = OrdoTheme.colors.textTertiary)
                        }
                        DotMatrixText((placeIndex + 1).toString().padStart(2, '0'), 58.dp)
                        Box(Modifier.padding(top = 18.dp)) {
                            if (placeIndex + 1 < placeCount) DotMatrixText((placeIndex + 2).toString().padStart(2, '0'), 14.dp, color = OrdoTheme.colors.textTertiary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(UiR.string.place_of, placeIndex + 1, placeCount), style = OrdoTheme.type.body, color = OrdoTheme.colors.textSecondary)
                }
            }
            val partners = list.ranking.places[placeIndex].items.filter { it != item.id }.map { list.item(it).title }
            if (partners.isNotEmpty()) {
                Text(
                    stringResource(R.string.shared_with, partners.joinToString(", ")),
                    style = OrdoTheme.type.label,
                    color = OrdoTheme.colors.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        } else {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.not_placed), style = OrdoTheme.type.sectionTitle, color = OrdoTheme.colors.textSecondary)
            Spacer(Modifier.height(14.dp))
            GlassPillButton(stringResource(R.string.place_now), { onPlace(item.id) }, icon = OrdoIcons.Duel, accent = true)
        }
        Spacer(Modifier.height(24.dp))
        NoteCard(item, vm)
        Spacer(Modifier.height(40.dp))
    }
    viewing?.let { PictureViewer(item.images, it) { viewing = null } }
}

/** The note, editable in place. Saves itself a moment after you stop typing. */
@Composable
private fun NoteCard(item: RankItem, vm: ItemViewModel?) {
    var editing by rememberSaveable(item.id.value) { mutableStateOf(false) }
    var text by rememberSaveable(item.id.value) { mutableStateOf(item.note) }
    LaunchedEffect(text) {
        delay(600)
        vm?.saveNote(item, text)
    }
    PillowSurface(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.Icon(OrdoIcons.Note, null, Modifier.height(18.dp), tint = OrdoTheme.colors.textSecondary)
                Text(stringResource(R.string.note), style = OrdoTheme.type.cardTitle, color = OrdoTheme.colors.textPrimary)
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(editing, transitionSpec = { fadeIn(OrdoMotion.smooth()) togetherWith fadeOut(OrdoMotion.smooth()) }, label = "note") { e ->
                if (e) {
                    OrdoTextField(text, { text = it }, "", singleLine = false, minHeight = 140.dp)
                } else {
                    Text(
                        text.ifBlank { stringResource(R.string.note_empty) },
                        style = OrdoTheme.type.body,
                        color = if (text.isBlank()) OrdoTheme.colors.textTertiary else OrdoTheme.colors.textSecondary,
                        modifier = Modifier.fillMaxWidth().clickable { editing = true },
                    )
                }
            }
        }
    }
}
