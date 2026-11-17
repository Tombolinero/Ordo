package io.github.tombolinero.ordo.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.tombolinero.ordo.core.data.OrdoRepository
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.component.OrdoBackground
import io.github.tombolinero.ordo.core.designsystem.component.OrdoTopBar
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.Clock
import io.github.tombolinero.ordo.core.model.Retention
import io.github.tombolinero.ordo.core.model.TrashEntry
import io.github.tombolinero.ordo.core.ui.EmptyState
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.ordoViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(private val repository: OrdoRepository) : ViewModel() {
    val entries: StateFlow<List<TrashEntry>> = repository.observeTrash().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(entry: TrashEntry) = viewModelScope.launch {
        when (entry) {
            is TrashEntry.List -> repository.restoreList(entry.list.id)
            is TrashEntry.Item -> repository.restoreItem(entry.item.id)
        }
    }

    fun deleteForever(entry: TrashEntry) = viewModelScope.launch { repository.deleteForever(entry) }

    fun emptyTrash() = viewModelScope.launch { repository.emptyTrash() }
}

@Composable
fun TrashRoute(onBack: () -> Unit) {
    val vm = ordoViewModel { TrashViewModel(it.repository) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    TrashScreen(entries, Clock.System.now(), onBack, vm::restore, vm::deleteForever, vm::emptyTrash)
}

@Composable
internal fun TrashScreen(
    entries: List<TrashEntry>,
    now: Long,
    onBack: () -> Unit,
    onRestore: (TrashEntry) -> Unit,
    onDelete: (TrashEntry) -> Unit,
    onEmpty: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    OrdoBackground {
        Column(Modifier.fillMaxSize()) {
            OrdoTopBar(
                stringResource(R.string.trash_title),
                navigation = { GlassIconButton(OrdoIcons.Back, stringResource(UiR.string.action_back), onBack) },
                action = if (entries.isEmpty()) null else ({ GlassIconButton(OrdoIcons.Trash, stringResource(R.string.trash_empty_action), { confirm = true }) }),
            )
            if (entries.isEmpty()) {
                EmptyState(stringResource(R.string.trash_empty_title), stringResource(R.string.trash_empty_hint), Modifier.padding(bottom = 80.dp))
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(entries, key = { e -> when (e) { is TrashEntry.List -> "l" + e.list.id.value; is TrashEntry.Item -> "i" + e.item.id.value } }) { e ->
                        TrashRow(e, now, onRestore, onDelete, Modifier.animateItem())
                    }
                }
            }
        }
    }
    if (confirm) {
        OrdoSheet(onDismiss = { confirm = false }) {
            Text(stringResource(R.string.trash_empty_confirm), style = OrdoTheme.type.body, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(4.dp))
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                GlassPillButton(stringResource(UiR.string.action_cancel), { confirm = false })
                GlassPillButton(stringResource(R.string.trash_empty_action), { confirm = false; onEmpty() }, icon = OrdoIcons.Trash, accent = true)
            }
        }
    }
}

@Composable
private fun TrashRow(entry: TrashEntry, now: Long, onRestore: (TrashEntry) -> Unit, onDelete: (TrashEntry) -> Unit, modifier: Modifier) {
    val daysLeft = (((entry.deletedAt + Retention.TRASH_MILLIS) - now) / 86_400_000L).coerceAtLeast(0).toInt()
    PillowSurface(modifier.fillMaxWidth(), shape = OrdoShapes.CardSmall, elevation = 8.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                when (entry) {
                    is TrashEntry.Item -> ItemImage(entry.item.image, entry.item.title, 44.dp)
                    is TrashEntry.List -> Box(Modifier.padding(4.dp)) { androidx.compose.material3.Icon(OrdoIcons.Lists, null, tint = OrdoTheme.colors.textSecondary) }
                }
                Column(Modifier.weight(1f)) {
                    val title = when (entry) { is TrashEntry.Item -> entry.item.title; is TrashEntry.List -> entry.list.title }
                    Text(title, style = OrdoTheme.type.itemTitle, color = OrdoTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = when (entry) {
                        is TrashEntry.Item -> stringResource(R.string.trash_from, entry.listTitle)
                        is TrashEntry.List -> stringResource(R.string.trash_list, androidx.compose.ui.res.pluralStringResource(UiR.plurals.entries_count, entry.itemCount, entry.itemCount))
                    }
                    Text("$sub · ${stringResource(R.string.trash_days_left, daysLeft)}", style = OrdoTheme.type.meta, color = OrdoTheme.colors.textSecondary)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(OrdoIcons.Trash, stringResource(R.string.trash_delete_forever), { onDelete(entry) })
                GlassPillButton(stringResource(R.string.trash_restore), { onRestore(entry) }, icon = OrdoIcons.Restore, accent = true)
            }
        }
    }
}
