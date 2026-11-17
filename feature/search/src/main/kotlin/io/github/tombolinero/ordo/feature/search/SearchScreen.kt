package io.github.tombolinero.ordo.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.tombolinero.ordo.core.data.OrdoRepository
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.component.OrdoBackground
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.SearchHit
import io.github.tombolinero.ordo.core.ui.EmptyState
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.OrdoTextField
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.SharedKeys
import io.github.tombolinero.ordo.core.ui.ordoViewModel
import io.github.tombolinero.ordo.core.ui.sharedBounds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(repository: OrdoRepository) : ViewModel() {
    val query = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val hits: StateFlow<List<SearchHit>> = query.debounce(120).flatMapLatest { repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun SearchRoute(onBack: () -> Unit, onOpenItem: (SearchHit) -> Unit) {
    val vm = ordoViewModel { SearchViewModel(it.repository) }
    val query by vm.query.collectAsStateWithLifecycle()
    val hits by vm.hits.collectAsStateWithLifecycle()
    SearchScreen(query, { vm.query.value = it }, hits, onBack, onOpenItem)
}

@Composable
internal fun SearchScreen(query: String, onQuery: (String) -> Unit, hits: List<SearchHit>, onBack: () -> Unit, onOpenItem: (SearchHit) -> Unit, autoFocus: Boolean = true) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) runCatching { focus.requestFocus() } }
    OrdoBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassIconButton(OrdoIcons.Back, stringResource(UiR.string.action_back), onBack, modifier = Modifier.padding(bottom = 4.dp))
                OrdoTextField(
                    query, onQuery, "", Modifier.weight(1f).focusRequester(focus),
                    placeholder = stringResource(R.string.search_hint), imeAction = ImeAction.Search,
                )
            }
            when {
                query.isBlank() -> EmptyState(stringResource(R.string.search_empty_title), stringResource(R.string.search_empty_hint), Modifier.padding(bottom = 80.dp))
                hits.isEmpty() -> EmptyState(stringResource(R.string.search_none_title), stringResource(R.string.search_none_hint, query.trim()), Modifier.padding(bottom = 80.dp))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    hits.groupBy { it.list.id }.forEach { (listId, group) ->
                        item(key = "h-" + listId.value) {
                            Text(
                                group.first().list.title.uppercase(),
                                style = OrdoTheme.type.overline,
                                color = OrdoTheme.colors.textTertiary,
                                modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp).animateItem(),
                            )
                        }
                        items(group, key = { "i-" + it.item.id.value }) { hit -> HitRow(hit, query, onOpenItem, Modifier.animateItem()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HitRow(hit: SearchHit, query: String, onOpen: (SearchHit) -> Unit, modifier: Modifier) {
    PillowSurface(modifier.fillMaxWidth(), shape = OrdoShapes.Row, elevation = 6.dp) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable { onOpen(hit) }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                DotMatrixText(hit.place?.toString()?.padStart(2, '0') ?: "-", 16.dp, color = if (hit.place == null) OrdoTheme.colors.textTertiary else OrdoTheme.colors.textPrimary)
            }
            ItemImage(hit.item.image, hit.item.title, 42.dp, Modifier.sharedBounds(SharedKeys.itemImage(hit.item.id.value)))
            Column(Modifier.weight(1f)) {
                Text(hit.item.title, style = OrdoTheme.type.itemTitle, color = OrdoTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val inNote = hit.item.note.contains(query.trim(), ignoreCase = true) && !hit.item.title.contains(query.trim(), ignoreCase = true)
                if (inNote) Text(hit.item.note, style = OrdoTheme.type.meta, color = OrdoTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
