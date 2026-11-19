package io.github.tombolinero.ordo.feature.item

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.tombolinero.ordo.core.data.CropRect
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ImageId
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.dataGraph

/** One picture while editing: already stored, or freshly picked (not imported yet). */
sealed interface PictureDraft {
    /** Stable identity for lists; the same picture is never added twice. */
    val key: String

    data class Stored(val id: ImageId) : PictureDraft {
        override val key get() = "id:${id.value}"
    }

    data class Picked(val uri: Uri, val crop: CropRect) : PictureDraft {
        override val key get() = "uri:$uri"
    }
}

/**
 * Picture strip of the edit sheet. Tapping a picture makes it the cover (yellow ring and
 * star), the small cross removes it, the last tile adds more.
 */
@Composable
internal fun PictureStrip(
    pictures: List<PictureDraft>,
    cover: Int,
    title: String,
    onCover: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)) {
        itemsIndexed(pictures, key = { _, p -> p.key }) { i, p ->
            val selected = i == cover
            val ring by animateColorAsState(if (selected) OrdoPalette.Yellow else Color.Transparent, OrdoMotion.smooth(), label = "ring")
            val scale by animateFloatAsState(if (selected) 1f else 0.92f, OrdoMotion.bouncy(), label = "scale")
            Box(Modifier.animateItem().size(84.dp)) {
                val shape = SmoothCornerShape(22.dp)
                Box(
                    Modifier.size(84.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                        .border(2.5.dp, ring, shape).padding(4.dp).clip(shape).clickable { onCover(i) },
                ) { PictureThumb(p, title, 76.dp) }
                if (selected) {
                    Badge(Modifier.align(Alignment.BottomStart), OrdoPalette.Yellow) {
                        Icon(OrdoIcons.Star, stringResource(R.string.cover), Modifier.size(12.dp), tint = OrdoPalette.YellowInk)
                    }
                }
                Badge(Modifier.align(Alignment.TopEnd).clickable { onRemove(i) }, Color(0xFF2A2A2A)) {
                    Icon(OrdoIcons.Close, stringResource(R.string.remove_picture), Modifier.size(12.dp), tint = Color.White)
                }
            }
        }
        item(key = "add") {
            Box(
                Modifier.animateItem().padding(4.dp).size(76.dp).clip(SmoothCornerShape(22.dp))
                    .background(Color(0xFF1C1C1C)).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Icon(OrdoIcons.Plus, stringResource(R.string.add_picture), Modifier.size(24.dp), tint = OrdoTheme.colors.accent) }
        }
    }
}

@Composable
private fun Badge(modifier: Modifier, color: Color, content: @Composable () -> Unit) {
    Box(modifier.size(24.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun PictureThumb(p: PictureDraft, title: String, size: Dp) = when (p) {
    is PictureDraft.Stored -> ItemImage(p.id, title, size, shape = SmoothCornerShape(18.dp))
    is PictureDraft.Picked -> AsyncImage(p.uri, null, Modifier.size(size), contentScale = ContentScale.Crop)
}

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

/** Full-screen pictures; swipe between them. */
@Composable
internal fun PictureViewer(images: List<ImageId>, start: Int, onClose: () -> Unit) {
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onClose)
        val context = LocalContext.current
        val pager = rememberPagerState(initialPage = start.coerceIn(0, (images.size - 1).coerceAtLeast(0))) { images.size }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize(), key = { images[it].value }) { page ->
                val file = remember(images[page]) { context.dataGraph.images.file(images[page]) }
                AsyncImage(file, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            GlassIconButton(
                OrdoIcons.Close, stringResource(UiR.string.action_back), onClose,
                Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(20.dp),
            )
            if (images.size > 1) {
                Row(
                    Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(images.size) { i ->
                        val active = i == pager.currentPage
                        val w by animateFloatAsState(if (active) 22f else 7f, OrdoMotion.bouncy(), label = "dot")
                        Box(Modifier.size(w.dp, 7.dp).clip(CircleShape).background(if (active) OrdoPalette.Yellow else Color.White.copy(alpha = 0.35f)))
                    }
                }
            }
        }
    }
}
