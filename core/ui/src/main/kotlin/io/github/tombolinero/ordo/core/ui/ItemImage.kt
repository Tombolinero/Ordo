package io.github.tombolinero.ordo.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.Urbanist
import io.github.tombolinero.ordo.core.model.ImageId
import kotlin.math.absoluteValue

/**
 * An entry's picture. Without a picture it shows a soft gradient with the first letter,
 * coloured consistently per title so the list stays calm and recognisable.
 */
@Composable
fun ItemImage(
    image: ImageId?,
    title: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = SmoothCornerShape(size * 0.32f),
) {
    val context = LocalContext.current
    Box(modifier.size(size).clip(shape).placeholderFill(title), contentAlignment = Alignment.Center) {
        if (image == null) {
            Text(
                title.trim().firstOrNull()?.uppercase() ?: "·",
                style = TextStyle(fontFamily = Urbanist, fontSize = (size.value * 0.42f).sp),
                color = Color.White.copy(alpha = 0.9f),
            )
        } else {
            val file = remember(image) { context.dataGraph.images.file(image) }
            AsyncImage(
                model = ImageRequest.Builder(context).data(file).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}

private val placeholderGradients = listOf(
    OrdoPalette.PurpleGlow to OrdoPalette.PurpleDeep,
    OrdoPalette.Violet to OrdoPalette.VioletMuted,
    Color(0xFFE2475A) to Color(0xFF5E0C24),
    Color(0xFF4A7CF0) to Color(0xFF0E1F63),
    Color(0xFF19B3A3) to Color(0xFF07393D),
    Color(0xFF8A8A8A) to Color(0xFF242424),
)

private fun Modifier.placeholderFill(seed: String) = drawWithCache {
    val (a, b) = placeholderGradients[seed.hashCode().absoluteValue % placeholderGradients.size]
    val brush = Brush.linearGradient(listOf(a, b), Offset.Zero, Offset(size.width, size.height))
    val shine = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), Offset(size.width * 0.28f, size.height * 0.22f), size.minDimension * 0.7f)
    onDrawBehind {
        drawRect(brush)
        drawRect(shine)
    }
}
