package io.github.tombolinero.ordo.feature.lists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.component.pressScale
import io.github.tombolinero.ordo.core.designsystem.component.secondaryContentColor
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ListSummary
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.pillow

/**
 * Home screen card of one list (decision E4 B): card colour of the list, title, counts,
 * the current number one and a big dot-matrix entry count.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListCard(
    summary: ListSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = summary.list.color.pillow()
    val secondary = style.secondaryContentColor()
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    PillowSurface(
        modifier
            .fillMaxWidth()
            .height(136.dp)
            .pressScale(interaction, 0.975f),
        style = style,
        shape = OrdoShapes.Card,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                )
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (summary.list.pinned) Icon(OrdoIcons.Pin, null, Modifier.size(16.dp), tint = secondary)
                    Text(summary.list.title, style = OrdoTheme.type.cardTitle, color = OrdoTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text(metaLine(summary), style = OrdoTheme.type.meta, color = secondary, maxLines = 1)
                Spacer(Modifier.weight(1f))
                val top = summary.topItem
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (top != null) {
                        ItemImage(top.image, top.title, 34.dp)
                        Column {
                            Text(stringResource(R.string.home_top), style = OrdoTheme.type.overline, color = secondary)
                            Text(top.title, style = OrdoTheme.type.label, color = OrdoTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        Text(stringResource(R.string.home_no_top), style = OrdoTheme.type.label, color = secondary)
                    }
                }
            }
            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomEnd) {
                DotMatrixText(summary.itemCount.coerceAtMost(99).toString().padStart(2, '0'), 30.dp)
            }
        }
    }
}

@Composable
private fun metaLine(s: ListSummary): String = buildList {
    add(pluralStringResource(UiR.plurals.entries_count, s.itemCount, s.itemCount))
    if (s.tieCount > 0) add(pluralStringResource(UiR.plurals.ties_count, s.tieCount, s.tieCount))
    if (s.unrankedCount > 0) add(pluralStringResource(UiR.plurals.unranked_count, s.unrankedCount, s.unrankedCount))
}.joinToString(" · ")
