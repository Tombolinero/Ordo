package io.github.tombolinero.ordo.feature.item

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.model.ImageId
import io.github.tombolinero.ordo.core.ui.ItemImage

/** Small thumbnails under the big picture on the entry page (only with 2+ pictures). */
@Composable
internal fun PictureRow(images: List<ImageId>, cover: ImageId?, title: String, onOpen: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        images.take(6).forEachIndexed { i, id ->
            val shape = SmoothCornerShape(14.dp)
            ItemImage(
                id, title, 48.dp,
                Modifier.border(2.dp, if (id == cover) OrdoPalette.Yellow.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f), shape)
                    .clickable { onOpen(i) },
                shape = shape,
            )
        }
    }
}
