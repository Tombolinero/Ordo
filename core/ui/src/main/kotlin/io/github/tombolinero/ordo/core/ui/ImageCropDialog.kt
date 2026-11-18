package io.github.tombolinero.ordo.core.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.tombolinero.ordo.core.data.CropRect
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Full-screen crop: the picture moves and zooms under a fixed square frame (the shape entries
 * use everywhere). Releasing the fingers springs the picture back so the frame is always covered.
 */
@Composable
fun ImageCropDialog(uri: Uri, onDismiss: () -> Unit, onCropped: (CropRect) -> Unit) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = runCatching { context.dataGraph.images.loadPreview(context, uri) }.getOrNull()
        if (bitmap == null) onDismiss()
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().padding(0.dp)) {
            Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black) }
            bitmap?.let { CropContent(it, onDismiss, onCropped) }
        }
    }
}

@Composable
private fun CropContent(bitmap: Bitmap, onDismiss: () -> Unit, onCropped: (CropRect) -> Unit) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Text(
            stringResource(R.string.crop_title),
            style = OrdoTheme.type.screenTitle,
            color = OrdoTheme.colors.textPrimary,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val frameSide = min(w, h) * 0.84f
            val frame = Rect(Offset((w - frameSide) / 2, (h - frameSide) / 2), Size(frameSide, frameSide))
            val minScale = max(frameSide / bitmap.width, frameSide / bitmap.height)
            val scale = remember { Animatable(minScale) }
            val offsetX = remember { Animatable(0f) }
            val offsetY = remember { Animatable(0f) }

            fun clampTargets(s: Float): Pair<Float, Float> =
                CropMath.clampOffset(offsetX.value, bitmap.width * s, frameSide) to
                    CropMath.clampOffset(offsetY.value, bitmap.height * s, frameSide)

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(bitmap) {
                        detectTransformGestures(
                            onGesture = { _, pan, zoom, _ ->
                                // Two fingers on the same spot can report a zoom of 0/0: ignore such events.
                                if (!zoom.isFinite() || zoom <= 0f || !pan.x.isFinite() || !pan.y.isFinite()) return@detectTransformGestures
                                scope.launch {
                                    scale.snapTo((scale.value * zoom).coerceIn(minScale * 0.85f, minScale * 6f))
                                    offsetX.snapTo(offsetX.value + pan.x)
                                    offsetY.snapTo(offsetY.value + pan.y)
                                }
                            },
                        )
                    }
                    .pointerInput(bitmap) {
                        // When all fingers lift, spring back inside the allowed range.
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) {
                                    val s = scale.value.coerceAtLeast(minScale)
                                    scope.launch { scale.animateTo(s, OrdoMotion.bouncy()) }
                                    val (tx, ty) = clampTargets(s)
                                    scope.launch { offsetX.animateTo(tx, OrdoMotion.bouncy()) }
                                    scope.launch { offsetY.animateTo(ty, OrdoMotion.bouncy()) }
                                }
                            }
                        }
                    },
            ) {
                val s = scale.value
                val iw = bitmap.width * s
                val ih = bitmap.height * s
                val left = frame.center.x - iw / 2 + offsetX.value
                val top = frame.center.y - ih / 2 + offsetY.value
                drawImage(image, dstOffset = IntOffset(left.toInt(), top.toInt()), dstSize = IntSize(iw.toInt(), ih.toInt()))
                val outline = SmoothCornerShape(36.dp).createOutline(frame.size, layoutDirection, this)
                val framePath = Path().apply {
                    when (outline) {
                        is androidx.compose.ui.graphics.Outline.Generic -> addPath(outline.path)
                        is androidx.compose.ui.graphics.Outline.Rounded -> addRoundRect(outline.roundRect)
                        is androidx.compose.ui.graphics.Outline.Rectangle -> addRect(outline.rect)
                    }
                    translate(frame.topLeft)
                }
                clipPath(framePath, ClipOp.Difference) { drawRect(Color.Black.copy(alpha = 0.68f)) }
                drawPath(framePath, OrdoPalette.Yellow.copy(alpha = 0.9f), style = Stroke(with(density) { 2.dp.toPx() }))
            }

            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                GlassPillButton(stringResource(R.string.action_cancel), onDismiss)
                GlassPillButton(stringResource(R.string.crop_apply), icon = OrdoIcons.Check, accent = true, onClick = {
                    val s = scale.value.coerceAtLeast(minScale)
                    val (ox, oy) = clampTargets(s)
                    val iw = bitmap.width * s
                    val ih = bitmap.height * s
                    onCropped(CropMath.cropRect(ox, oy, iw, ih, frameSide))
                })
            }
        }
    }
}

/**
 * Pure maths of the crop screen, kept separate so it can be tested with awkward numbers.
 * Float rounding can make "image size − frame size" a tiny negative number when the image
 * exactly fills the frame; every range below is guarded against that.
 */
internal object CropMath {
    /** Keeps the picture covering the frame: offset limited to ±(image − frame) / 2. */
    fun clampOffset(offset: Float, imageSide: Float, frameSide: Float): Float {
        val limit = ((imageSide - frameSide) / 2f).coerceAtLeast(0f)
        if (!limit.isFinite() || limit == 0f || !offset.isFinite()) return 0f
        return offset.coerceIn(-limit, limit)
    }

    /** The part of the picture inside the frame, as fractions of the picture. */
    fun cropRect(offsetX: Float, offsetY: Float, imageW: Float, imageH: Float, frameSide: Float): CropRect {
        if (!imageW.isFinite() || !imageH.isFinite() || !frameSide.isFinite() || frameSide <= 0f) return CropRect.Full
        val w = imageW.coerceAtLeast(frameSide).coerceAtLeast(1f)
        val h = imageH.coerceAtLeast(frameSide).coerceAtLeast(1f)
        val left = ((w - frameSide) / 2f - (if (offsetX.isFinite()) offsetX else 0f)) / w
        val top = ((h - frameSide) / 2f - (if (offsetY.isFinite()) offsetY else 0f)) / h
        val l = left.coerceIn(0f, 0.99f)
        val t = top.coerceIn(0f, 0.99f)
        val r = (left + frameSide / w).coerceIn(l + 0.01f, 1f)
        val b = (top + frameSide / h).coerceIn(t + 0.01f, 1f)
        return CropRect(l, t, r, b)
    }
}
