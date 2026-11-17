package io.github.tombolinero.ordo.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import kotlinx.coroutines.delay

/** One message with an optional "Undo" action, shown as a floating pill. */
data class UndoMessage(val text: String, val undoLabel: String? = null, val id: Long = System.nanoTime(), val onUndo: (() -> Unit)? = null)

@Stable
class UndoState {
    var current by mutableStateOf<UndoMessage?>(null)
        private set

    fun show(message: UndoMessage) {
        current = message
    }

    fun dismiss(id: Long) {
        if (current?.id == id) current = null
    }
}

@Composable
fun rememberUndoState() = remember { UndoState() }

/** Floating pill with the message and a yellow "Undo"; disappears after five seconds. */
@Composable
fun UndoBar(state: UndoState, modifier: Modifier = Modifier) {
    val message = state.current
    LaunchedEffect(message?.id) {
        if (message != null) {
            delay(5_000)
            state.dismiss(message.id)
        }
    }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = slideInVertically(OrdoMotion.bouncy(IntOffset.VisibilityThreshold)) { it } + fadeIn(OrdoMotion.smooth()),
        exit = slideOutVertically(OrdoMotion.smooth(IntOffset.VisibilityThreshold)) { it } + fadeOut(OrdoMotion.smooth()),
    ) {
        val shown = remember(message?.id) { message } ?: return@AnimatedVisibility
        Row(
            Modifier
                .widthIn(max = 420.dp)
                .clip(OrdoShapes.Pill)
                .drawBehind { drawRect(Color(0xFF222222)) }
                .padding(start = 22.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(shown.text, style = OrdoTheme.type.bodySmall, color = OrdoTheme.colors.textPrimary, modifier = Modifier.weight(1f, fill = false))
            if (shown.onUndo != null && shown.undoLabel != null) {
                Row(
                    Modifier
                        .clip(OrdoShapes.Pill)
                        .clickable {
                            shown.onUndo.invoke()
                            state.dismiss(shown.id)
                        }
                        .drawBehind { drawRect(OrdoPalette.Yellow) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(OrdoIcons.Undo, null, Modifier.size(16.dp), tint = OrdoPalette.YellowInk)
                    Text(shown.undoLabel, style = OrdoTheme.type.button.copy(fontSize = OrdoTheme.type.label.fontSize), color = OrdoPalette.YellowInk)
                }
            } else {
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

/** Calm empty state: a big dot-matrix "00" and two lines of text. */
@Composable
fun EmptyState(title: String, hint: String, modifier: Modifier = Modifier, number: String = "00") {
    Column(
        modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(true, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            DotMatrixText(number, 56.dp, color = OrdoTheme.colors.textTertiary)
        }
        Spacer(Modifier.height(28.dp))
        Text(title, style = OrdoTheme.type.sectionTitle, color = OrdoTheme.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(hint, style = OrdoTheme.type.bodySmall, color = OrdoTheme.colors.textSecondary, textAlign = TextAlign.Center)
    }
}
