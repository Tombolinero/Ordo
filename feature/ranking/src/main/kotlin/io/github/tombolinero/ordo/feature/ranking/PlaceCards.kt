package io.github.tombolinero.ordo.feature.ranking

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.PillowStyle
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.component.pressScale
import io.github.tombolinero.ordo.core.designsystem.component.secondaryContentColor
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.SharedKeys
import io.github.tombolinero.ordo.core.ui.sharedBounds

/** How prominently a place is shown (decision E5 A: podium for the top three). */
enum class PlaceSize(
    val number: Dp,
    val image: Dp,
    val tieImage: Dp,
    val minHeight: Dp,
    val padding: Dp,
) {
    Hero(number = 34.dp, image = 88.dp, tieImage = 56.dp, minHeight = 132.dp, padding = 20.dp),
    Large(number = 24.dp, image = 60.dp, tieImage = 44.dp, minHeight = 96.dp, padding = 16.dp),
    Compact(number = 16.dp, image = 44.dp, tieImage = 36.dp, minHeight = 70.dp, padding = 12.dp),
}

fun sizeForPlace(index: Int): PlaceSize = when (index) {
    0 -> PlaceSize.Hero
    1, 2 -> PlaceSize.Large
    else -> PlaceSize.Compact
}

/**
 * One place of the ranking: number, picture(s), title(s). Several entries on one place are
 * shown as one card with a yellow line joining them (decision E2 A).
 */
@Composable
fun PlaceCard(
    number: Int,
    members: List<RankItem>,
    size: PlaceSize,
    style: PillowStyle,
    onOpen: (RankItem) -> Unit,
    onLongPress: (RankItem) -> Unit,
    modifier: Modifier = Modifier,
    numberColor: Color = Color.White,
    trailing: (@Composable () -> Unit)? = null,
) {
    PillowSurface(
        modifier.fillMaxWidth(),
        style = style,
        shape = when (size) {
            PlaceSize.Hero -> OrdoShapes.Card
            PlaceSize.Large -> OrdoShapes.CardSmall
            PlaceSize.Compact -> OrdoShapes.Row
        },
        elevation = if (size == PlaceSize.Compact) 8.dp else 14.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = size.minHeight)
                .padding(start = size.padding + 2.dp, end = size.padding, top = size.padding * 0.7f, bottom = size.padding * 0.7f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (size == PlaceSize.Compact) 12.dp else 16.dp),
        ) {
            Box(Modifier.width(size.number * 1.25f), contentAlignment = Alignment.Center) {
                DotMatrixText(number.toString().padStart(2, '0'), size.number, color = numberColor)
            }
            if (members.size == 1) {
                EntryLine(members.single(), size.image, size, style, onOpen, onLongPress, Modifier.weight(1f))
            } else {
                Row(Modifier.weight(1f).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .padding(vertical = 6.dp)
                            .width(2.5.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .drawBehind { drawRect(OrdoPalette.Yellow) },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEach { EntryLine(it, size.tieImage, size, style, onOpen, onLongPress, Modifier.fillMaxWidth(), showNote = false) }
                    }
                }
            }
            trailing?.invoke()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryLine(
    item: RankItem,
    imageSize: Dp,
    size: PlaceSize,
    style: PillowStyle,
    onOpen: (RankItem) -> Unit,
    onLongPress: (RankItem) -> Unit,
    modifier: Modifier,
    showNote: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .pressScale(interaction, 0.98f)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onOpen(item) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress(item)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ItemImage(item.image, item.title, imageSize, Modifier.sharedBounds(SharedKeys.itemImage(item.id.value)))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = when (size) {
                    PlaceSize.Hero -> OrdoTheme.type.cardTitle
                    else -> OrdoTheme.type.itemTitle
                },
                color = OrdoTheme.colors.textPrimary,
                maxLines = if (size == PlaceSize.Hero) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedBounds(SharedKeys.itemTitle(item.id.value)),
            )
            if (showNote && item.note.isNotBlank()) {
                Text(
                    item.note,
                    style = if (size == PlaceSize.Compact) OrdoTheme.type.meta else OrdoTheme.type.label,
                    color = style.secondaryContentColor(),
                    maxLines = if (size == PlaceSize.Hero) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
