package io.github.tombolinero.ordo.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ImageId
import kotlinx.coroutines.launch

/** One picture in the viewer, with the entry it belongs to (shown as caption). */
data class ViewerPicture(val id: ImageId, val caption: String = "")

/** Full-screen pictures; swipe between them, pinch or double-tap to zoom. */
@Composable
fun PictureViewer(images: List<ImageId>, start: Int, onClose: () -> Unit) =
    PictureViewer(images.map { ViewerPicture(it) }, start, onClose, captions = false)

@Composable
fun PictureViewer(pictures: List<ViewerPicture>, start: Int, onClose: () -> Unit, captions: Boolean = true) {
    if (pictures.isEmpty()) return
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onClose)
        var zoomed by remember { mutableStateOf(false) }
        val pager = rememberPagerState(initialPage = start.coerceIn(0, pictures.size - 1)) { pictures.size }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            // While zoomed in, one finger pans the picture instead of swiping to the next one.
            HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = !zoomed, key = { "${it}_${pictures[it].id.value}" }) { page ->
                ZoomableImage(pictures[page].id, onZoomChanged = { if (page == pager.currentPage) zoomed = it })
            }
            GlassIconButton(
                OrdoIcons.Close, stringResource(R.string.action_back), onClose,
                Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(20.dp),
            )
            Column(
                Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (captions) {
                    AnimatedContent(pictures[pager.currentPage].caption, transitionSpec = { fadeIn(OrdoMotion.smooth()) togetherWith fadeOut(OrdoMotion.smooth()) }, label = "caption") { c ->
                        Text(c, style = OrdoTheme.type.cardTitle, color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
                if (pictures.size > 1) {
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(pictures.size) { i ->
                            val active = i == pager.currentPage
                            val w by animateFloatAsState(if (active) 22f else 7f, OrdoMotion.bouncy(), label = "dot")
                            Box(Modifier.size(w.dp, 7.dp).clip(CircleShape).background(if (active) OrdoPalette.Yellow else Color.White.copy(alpha = 0.35f)))
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

@Composable
private fun ZoomableImage(image: ImageId, onZoomChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val file = remember(image) { context.dataGraph.images.file(image) }
    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    fun bounds(s: Float) = Offset(size.width * (s - 1f) / 2f, size.height * (s - 1f) / 2f)

    fun zoomTo(target: Float, focus: Offset) = scope.launch {
        val b = bounds(target)
        // Keep the tapped point under the finger.
        val center = Offset(size.width / 2f, size.height / 2f)
        val x = if (target == 1f) 0f else ((center.x - focus.x) * (target - 1f)).coerceIn(-b.x, b.x)
        val y = if (target == 1f) 0f else ((center.y - focus.y) * (target - 1f)).coerceIn(-b.y, b.y)
        onZoomChanged(target > 1f)
        launch { scale.animateTo(target, OrdoMotion.smooth()) }
        launch { offsetX.animateTo(x, OrdoMotion.smooth()) }
        offsetY.animateTo(y, OrdoMotion.smooth())
    }

    AsyncImage(
        model = file,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(image) {
                detectTapGestures(onDoubleTap = { p -> zoomTo(if (scale.value > 1.05f) 1f else DOUBLE_TAP_ZOOM, p) })
            }
            .pointerInput(image) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        // Only take over when pinching or already zoomed; otherwise the pager swipes.
                        if (zoom != 1f || scale.value > 1f) {
                            val s = (scale.value * zoom).coerceIn(1f, MAX_ZOOM)
                            val b = bounds(s)
                            scope.launch {
                                scale.snapTo(s)
                                offsetX.snapTo((offsetX.value + pan.x).coerceIn(-b.x, b.x))
                                offsetY.snapTo((offsetY.value + pan.y).coerceIn(-b.y, b.y))
                            }
                            onZoomChanged(s > 1f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (scale.value < 1.05f) zoomTo(1f, Offset.Zero)
                }
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = offsetX.value
                translationY = offsetY.value
            },
    )
}
