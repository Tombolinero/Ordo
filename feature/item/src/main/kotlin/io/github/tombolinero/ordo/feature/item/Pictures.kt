package io.github.tombolinero.ordo.feature.item

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.model.ImageId
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.dataGraph

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
