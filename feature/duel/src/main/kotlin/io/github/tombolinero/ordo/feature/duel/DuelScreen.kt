package io.github.tombolinero.ordo.feature.duel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.component.ProgressRing
import io.github.tombolinero.ordo.core.designsystem.component.RankWheel
import io.github.tombolinero.ordo.core.designsystem.component.pressScale
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.Duel
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.ordoViewModel
import io.github.tombolinero.ordo.core.ui.pillow

@Composable
fun DuelRoute(listId: ListId, itemId: ItemId, onClose: () -> Unit) {
    val vm = ordoViewModel(key = "duel-${itemId.value}") { DuelViewModel(it.repository, listId, itemId) }
    val state by vm.state.collectAsStateWithLifecycle()
    DuelScreen(state, onAnswer = vm::answer, onUndo = vm::undo, onClose = onClose)
}

/**
 * "A or B?": the new entry (top, in the list colour) against an existing place (bottom).
 * Tap the better one, or "Equally good" to share the place. The ring shows how many
 * questions are left at most; at the end the rank wheel sweeps to the new place.
 */
@Composable
internal fun DuelScreen(state: DuelUiState, onAnswer: (Duel.Answer) -> Unit, onUndo: () -> Unit, onClose: () -> Unit) {
    val duel = state.duel
    val newItem = state.newItem
    OrdoBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
            OrdoTopBar(
                stringResource(R.string.duel_title),
                navigation = { GlassIconButton(OrdoIcons.Close, stringResource(UiR.string.action_cancel), onClose) },
                action = {
                    val asked = duel?.answered ?: 0
                    val left = duel?.remainingAtMost ?: 0
                    ProgressRing(if (asked + left == 0) 1f else asked.toFloat() / (asked + left), diameter = 48.dp) {
                        DotMatrixText(left.toString(), 12.dp)
                    }
                },
            )
            if (duel == null || newItem == null) return@Column
            AnimatedContent(
                targetState = state.finalPlace,
                transitionSpec = { (fadeIn(OrdoMotion.smooth()) + scaleIn(OrdoMotion.bouncy(), initialScale = 0.92f)) togetherWith fadeOut(OrdoMotion.smooth()) },
                label = "phase",
                modifier = Modifier.weight(1f),
            ) { final ->
                if (final != null) {
                    Result(final, state.finalIsTie, state.base.places.size + if (state.finalIsTie) 0 else 1, onClose)
                } else {
                    Question(state, newItem, onAnswer, onUndo)
                }
            }
        }
    }
}

@Composable
private fun Question(state: DuelUiState, newItem: RankItem, onAnswer: (Duel.Answer) -> Unit, onUndo: () -> Unit) {
    val duel = state.duel ?: return
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.duel_question),
            style = OrdoTheme.type.sectionTitle,
            color = OrdoTheme.colors.textPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
        )
        Choice(
            label = stringResource(R.string.duel_new),
            items = listOf(newItem),
            style = state.list?.color?.pillow() ?: PillowStyle.Magenta,
            onClick = { onAnswer(Duel.Answer.NewIsBetter) },
        )
        Spacer(Modifier.height(14.dp))
        GlassPillButton(stringResource(R.string.duel_equal), { onAnswer(Duel.Answer.Equal) }, icon = OrdoIcons.Merge)
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = duel.opponent,
            transitionSpec = {
                val down = (targetState ?: 0) > (initialState ?: 0)
                (slideInVertically(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { if (down) it / 2 else -it / 2 } + fadeIn(OrdoMotion.smooth())) togetherWith
                    (slideOutVertically(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { if (down) -it / 2 else it / 2 } + fadeOut(OrdoMotion.smooth()))
            },
            label = "opponent",
        ) { opponent ->
            if (opponent != null) {
                Choice(
                    label = stringResource(R.string.duel_place, opponent + 1),
                    items = state.base.places[opponent].items.mapNotNull(state.items::get),
                    style = PillowStyle.Graphite,
                    onClick = { onAnswer(Duel.Answer.NewIsWorse) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (duel.answered > 0) {
            GlassPillButton(stringResource(R.string.duel_back), onUndo, icon = OrdoIcons.Undo, modifier = Modifier.padding(bottom = 18.dp))
        }
    }
}

@Composable
private fun Choice(label: String, items: List<RankItem>, style: PillowStyle, onClick: () -> Unit) {
    val first = items.firstOrNull() ?: return
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    PillowSurface(
        Modifier.fillMaxWidth().heightIn(min = 176.dp).pressScale(interaction, 0.97f),
        style = style,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(interaction, indication = null, role = Role.Button) {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onClick()
                }
                .padding(20.dp),
        ) {
            Text(label.uppercase(), style = OrdoTheme.type.overline, color = OrdoTheme.colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ItemImage(first.image, first.title, 96.dp)
                Column(Modifier.weight(1f)) {
                    Text(first.title, style = OrdoTheme.type.cardTitle, color = OrdoTheme.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (first.note.isNotBlank()) {
                        Text(first.note, style = OrdoTheme.type.label, color = OrdoTheme.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (items.size > 1) {
                        Text(stringResource(R.string.duel_more, items.size - 1), style = OrdoTheme.type.label, color = OrdoTheme.colors.accent, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Result(place: Int, tie: Boolean, total: Int, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            if (tie) stringResource(R.string.duel_result_tie, place) else stringResource(R.string.duel_result),
            style = OrdoTheme.type.sectionTitle,
            color = OrdoTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        RankWheel(count = total.coerceAtLeast(1), position = place, diameter = 280.dp) {
            Box(contentAlignment = Alignment.Center) { DotMatrixText(place.toString().padStart(2, '0'), 72.dp) }
        }
        Spacer(Modifier.height(36.dp))
        GlassPillButton(stringResource(R.string.duel_done), onDone, icon = OrdoIcons.Check, accent = true)
        Spacer(Modifier.height(60.dp))
    }
}
