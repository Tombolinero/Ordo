package io.github.tombolinero.ordo.feature.duel

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.component.OrdoBackground
import io.github.tombolinero.ordo.core.designsystem.component.OrdoTopBar
import io.github.tombolinero.ordo.core.designsystem.component.PillowStyle
import io.github.tombolinero.ordo.core.designsystem.component.ProgressRing
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.Duel
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.Rematch
import io.github.tombolinero.ordo.core.ui.EmptyState
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.ordoViewModel
import io.github.tombolinero.ordo.core.ui.pillow

@Composable
fun RematchRoute(listId: ListId, onClose: () -> Unit) {
    val vm = ordoViewModel(key = "rematch-${listId.value}") { RematchViewModel(it.repository, listId) }
    val state by vm.state.collectAsStateWithLifecycle()
    RematchScreen(state, vm::start, vm::answer, vm::undo, vm::restorePrevious, onClose)
}

/**
 * Rematch: the whole list is sorted again from scratch, two entries at a time. The ring shows
 * how many entries already have their new place; the old order stays until the end.
 */
@Composable
internal fun RematchScreen(
    state: RematchUiState,
    onStart: () -> Unit,
    onAnswer: (Duel.Answer) -> Unit,
    onUndo: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
) {
    var confirmStop by remember { mutableStateOf(false) }
    val r = state.rematch
    val inProgress = r != null && !state.saved && r.answered > 0
    val close = { if (inProgress) confirmStop = true else onClose() }
    BackHandler(enabled = inProgress) { confirmStop = true }

    OrdoBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
            OrdoTopBar(
                stringResource(R.string.rematch_title),
                navigation = { GlassIconButton(OrdoIcons.Close, stringResource(UiR.string.action_cancel), close) },
                action = if (r == null || state.saved) null else ({
                    ProgressRing(r.placedCount / r.total.coerceAtLeast(1).toFloat(), diameter = 48.dp) {
                        DotMatrixText(r.placedCount.coerceAtMost(99).toString(), 12.dp)
                    }
                }),
            )
            val phase = when {
                state.list == null -> 0
                r == null -> 1
                state.saved -> 3
                else -> 2
            }
            AnimatedContent(
                targetState = phase,
                transitionSpec = { (fadeIn(OrdoMotion.smooth()) + scaleIn(OrdoMotion.bouncy(), initialScale = 0.94f)) togetherWith fadeOut(OrdoMotion.smooth()) },
                label = "phase",
                modifier = Modifier.weight(1f),
            ) { p ->
                when (p) {
                    1 -> Intro(state, onStart)
                    2 -> r?.let { Question(state, it, onAnswer, onUndo) }
                    3 -> Done(state, onRestore, onClose)
                    else -> Unit
                }
            }
        }
    }
    if (confirmStop) {
        OrdoSheet(onDismiss = { confirmStop = false }) {
            Text(stringResource(R.string.rematch_stop_title), style = OrdoTheme.type.screenTitle, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            Text(stringResource(R.string.rematch_stop_hint), style = OrdoTheme.type.body, color = OrdoTheme.colors.textSecondary, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                GlassPillButton(stringResource(R.string.rematch_stop), { confirmStop = false; onClose() })
                GlassPillButton(stringResource(R.string.rematch_continue), { confirmStop = false }, accent = true)
            }
        }
    }
}

@Composable
private fun Intro(state: RematchUiState, onStart: () -> Unit) {
    val n = state.entryCount
    if (n < 2) {
        EmptyState(stringResource(R.string.rematch_title), stringResource(R.string.rematch_too_small), number = n.toString().padStart(2, '0'))
        return
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        DotMatrixText(n.coerceAtMost(99).toString().padStart(2, '0'), 64.dp)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.rematch_intro_title), style = OrdoTheme.type.sectionTitle, color = OrdoTheme.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.rematch_count, n, Rematch.maxQuestions(n)), style = OrdoTheme.type.label, color = OrdoTheme.colors.accent, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.rematch_intro_hint), style = OrdoTheme.type.bodySmall, color = OrdoTheme.colors.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        GlassPillButton(stringResource(R.string.rematch_start), onStart, icon = OrdoIcons.Duel, accent = true)
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun Question(state: RematchUiState, r: Rematch, onAnswer: (Duel.Answer) -> Unit, onUndo: () -> Unit) {
    val current = r.current?.let(state.items::get) ?: return
    val opponentPlace = r.opponent ?: return
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.duel_question), style = OrdoTheme.type.sectionTitle, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        Text(stringResource(R.string.rematch_progress, r.placedCount, r.total), style = OrdoTheme.type.meta, color = OrdoTheme.colors.textTertiary, modifier = Modifier.padding(bottom = 16.dp))
        AnimatedContent(
            targetState = current.id,
            transitionSpec = { (slideInVertically(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { -it / 3 } + fadeIn(OrdoMotion.smooth())) togetherWith fadeOut(OrdoMotion.smooth()) },
            label = "a",
        ) { _ ->
            Choice(stringResource(R.string.rematch_option_a), listOf(current), state.list?.color?.pillow() ?: PillowStyle.Magenta) { onAnswer(Duel.Answer.NewIsBetter) }
        }
        Spacer(Modifier.height(14.dp))
        GlassPillButton(stringResource(R.string.duel_equal), { onAnswer(Duel.Answer.Equal) }, icon = OrdoIcons.Merge)
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = r.sorted.places[opponentPlace].items.first(),
            transitionSpec = { (slideInVertically(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { it / 3 } + fadeIn(OrdoMotion.smooth())) togetherWith (slideOutVertically(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { -it / 3 } + fadeOut(OrdoMotion.smooth())) },
            label = "b",
        ) { _ ->
            Choice(stringResource(R.string.rematch_option_b), r.sorted.places[opponentPlace].items.mapNotNull(state.items::get), PillowStyle.Graphite) { onAnswer(Duel.Answer.NewIsWorse) }
        }
        Spacer(Modifier.weight(1f))
        if (r.canUndo) GlassPillButton(stringResource(R.string.duel_back), onUndo, icon = OrdoIcons.Undo, modifier = Modifier.padding(bottom = 18.dp))
    }
}

@Composable
private fun Done(state: RematchUiState, onRestore: () -> Unit, onClose: () -> Unit) {
    val result = state.rematch?.result ?: return
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            stringResource(if (state.restored) R.string.rematch_restored else R.string.rematch_done),
            style = OrdoTheme.type.sectionTitle, color = OrdoTheme.colors.textPrimary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (!state.restored) {
            result.places.take(5).forEachIndexed { i, place ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.width(34.dp), horizontalArrangement = Arrangement.Center) {
                        DotMatrixText((i + 1).toString().padStart(2, '0'), 16.dp, color = if (i == 0) OrdoPalette.Yellow else OrdoTheme.colors.textPrimary)
                    }
                    val first = state.items.getValue(place.items.first())
                    ItemImage(first.image, first.title, 40.dp)
                    Text(
                        place.items.joinToString(" = ") { state.items.getValue(it).title },
                        Modifier.weight(1f), style = OrdoTheme.type.itemTitle, color = OrdoTheme.colors.textPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        GlassPillButton(stringResource(R.string.duel_done), onClose, icon = OrdoIcons.Check, accent = true)
        if (!state.restored) {
            Spacer(Modifier.height(12.dp))
            GlassPillButton(stringResource(R.string.rematch_restore), onRestore, icon = OrdoIcons.Undo)
        }
        Spacer(Modifier.height(60.dp))
    }
}
