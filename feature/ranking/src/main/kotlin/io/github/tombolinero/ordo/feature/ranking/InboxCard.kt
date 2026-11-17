package io.github.tombolinero.ordo.feature.ranking

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.SharedKeys
import io.github.tombolinero.ordo.core.ui.sharedBounds

/** An entry waiting in "Not placed yet": dimmer card, no number, a "Place" button. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlaceCardInbox(item: RankItem, onOpen: (RankItem) -> Unit, onLongPress: (RankItem) -> Unit, action: @Composable () -> Unit) {
    PillowSurface(Modifier.fillMaxWidth(), shape = OrdoShapes.Row, elevation = 6.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .combinedClickable(onClick = { onOpen(item) }, onLongClick = { onLongPress(item) })
                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ItemImage(item.image, item.title, 44.dp, Modifier.sharedBounds(SharedKeys.itemImage(item.id.value)))
            Text(
                item.title,
                Modifier.weight(1f).sharedBounds(SharedKeys.itemTitle(item.id.value)),
                style = OrdoTheme.type.itemTitle,
                color = OrdoTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            action()
        }
    }
}
