package io.github.tombolinero.ordo.feature.ranking

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.Ranking
import io.github.tombolinero.ordo.core.ui.ItemImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MERGE_ZONE_START = 0.28f
private const val MERGE_ZONE_END = 0.72f
private const val MERGE_DWELL_MS = 380L
private val RowSpacing = 10.dp

/** Live state of one drag. Positions are in pixels relative to the list viewport. */
@Stable
internal class DragSession(
    val key: String,
    val fromIndex: Int,
    val height: Int,
    startTop: Float,
) {
    var visualTop by mutableFloatStateOf(startTop)
    var targetIndex by mutableIntStateOf(fromIndex)
    var mergeCandidate by mutableStateOf<Int?>(null)
    var mergeArmed by mutableStateOf(false)
    var settling by mutableStateOf(false)
    var dwell: Job? = null
}

private fun placeKey(place: Ranking.Place) = "place-" + place.items.first().value

/**
 * The ranking in sort mode (decision E1 C): uniform rows with a grip. Drag a row by its grip
 * (or long-press anywhere on it); the others glide out of the way on springs. Holding a row
 * over the middle of another one for a moment arms a merge (the target lights up yellow):
 * dropping then makes both share the place. Near the top/bottom edge the list scrolls along.
 *
 * The dragged row is drawn as a floating copy above the list, so it never disappears when the
 * list scrolls its original slot out of view.
 */
@Composable
fun SortableRanking(
    ranking: Ranking,
    items: Map<ItemId, RankItem>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onMove: (from: Int, to: Int) -> Unit,
    onMerge: (source: Int, target: Int) -> Unit,
    onDetach: (ItemId) -> Unit,
    modifier: Modifier = Modifier,
    header: LazyListScope.() -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val spacingPx = with(density) { RowSpacing.toPx() }
    val edgePx = with(density) { 96.dp.toPx() }
    var session by remember { mutableStateOf<DragSession?>(null) }
    var suppressPlacement by remember { mutableStateOf(false) }
    val overlayY = remember { Animatable(0f) }
    val overlayScale = remember { Animatable(1f) }

    fun layoutOf(key: String) = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    fun placeLayouts() = listState.layoutInfo.visibleItemsInfo
        .mapNotNull { info -> (info.key as? String)?.takeIf { it.startsWith("place-") }?.let { k -> ranking.places.indexOfFirst { placeKey(it) == k }.takeIf { it >= 0 }?.let { it to info } } }

    /** Where a non-dragged place currently is drawn (layout position + displacement). */
    fun displacementOf(index: Int, s: DragSession): Float {
        val shift = s.height + spacingPx
        return when {
            s.targetIndex > s.fromIndex && index in (s.fromIndex + 1)..s.targetIndex -> -shift
            s.targetIndex < s.fromIndex && index in s.targetIndex until s.fromIndex -> shift
            else -> 0f
        }
    }

    /** The gap goes where the row's centre is, measured against the other rows' layout midpoints. */
    fun slotFor(s: DragSession, others: List<Pair<Int, androidx.compose.foundation.lazy.LazyListItemInfo>>, center: Float): Int {
        val before = others.count { (_, info) -> info.offset + info.size / 2f < center }
        val firstVisible = others.minOfOrNull { it.first } ?: 0
        val hiddenAbove = (0 until firstVisible).count { it != s.fromIndex }
        val target = (hiddenAbove + before).coerceIn(0, ranking.places.lastIndex)
        if (target != s.targetIndex) haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        return target
    }

    fun update(s: DragSession) {
        val center = s.visualTop + s.height / 2f
        val others = placeLayouts().filter { it.first != s.fromIndex }
        // 1) Hovering the middle of another row → merge candidate (keeps the layout frozen).
        val hover = others.firstOrNull { (index, info) ->
            val top = info.offset + displacementOf(index, s)
            center in (top + info.size * MERGE_ZONE_START)..(top + info.size * MERGE_ZONE_END)
        }?.first
        if (hover != s.mergeCandidate) {
            s.mergeCandidate = hover
            s.mergeArmed = false
            s.dwell?.cancel()
            if (hover != null) {
                // Timed on display frames (not wall-clock delay) so holding still is measured exactly.
                s.dwell = scope.launch {
                    val startMs = withFrameMillis { it }
                    while (withFrameMillis { it } - startMs < MERGE_DWELL_MS) Unit
                    s.mergeArmed = true
                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                }
            }
        }
        if (hover != null) return
        s.targetIndex = slotFor(s, others, center)
    }

    fun start(index: Int) {
        if (session != null) return
        val place = ranking.places.getOrNull(index) ?: return
        val info = layoutOf(placeKey(place)) ?: return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val s = DragSession(placeKey(place), index, info.size, info.offset.toFloat())
        session = s
        scope.launch {
            overlayY.snapTo(s.visualTop)
            overlayScale.animateTo(1.035f, OrdoMotion.bouncy())
        }
        // Auto-scroll while the row is held near an edge.
        scope.launch {
            while (isActive && session === s && !s.settling) {
                withFrameNanos { }
                val info = listState.layoutInfo
                val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                val top = s.visualTop - info.viewportStartOffset
                val bottom = top + s.height
                val speed = when {
                    top < edgePx -> -((edgePx - top) / edgePx).coerceIn(0f, 1f) * 22f
                    bottom > viewport - edgePx -> ((bottom - (viewport - edgePx)) / edgePx).coerceIn(0f, 1f) * 22f
                    else -> 0f
                }
                if (speed != 0f) {
                    listState.scrollBy(speed)
                    update(s)
                }
            }
        }
    }

    fun drag(dy: Float) {
        val s = session ?: return
        if (s.settling) return
        s.visualTop += dy
        scope.launch { overlayY.snapTo(s.visualTop) }
        update(s)
    }

    fun end() {
        val s = session ?: return
        if (s.settling) return
        s.settling = true
        s.dwell?.cancel()
        val merge = s.mergeCandidate.takeIf { s.mergeArmed }
        if (merge == null && s.mergeCandidate != null) {
            // Released over a row before the merge armed: snap to the nearest slot instead.
            s.mergeCandidate = null
            s.targetIndex = slotFor(s, placeLayouts().filter { it.first != s.fromIndex }, s.visualTop + s.height / 2f)
        }
        scope.launch {
            val layouts = placeLayouts().toMap()
            val destination: Float? = when {
                merge != null -> layouts[merge]?.let { it.offset + displacementOf(merge, s) }
                s.targetIndex == s.fromIndex -> layouts[s.fromIndex]?.offset?.toFloat()
                s.targetIndex > s.fromIndex -> layouts[s.targetIndex]?.let { it.offset + it.size - s.height.toFloat() }
                else -> layouts[s.targetIndex]?.offset?.toFloat()
            }
            if (destination != null) {
                launch { overlayScale.animateTo(if (merge != null) 0.92f else 1f, OrdoMotion.smooth()) }
                overlayY.animateTo(destination, OrdoMotion.smooth(0.5f))
            }
            haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
            suppressPlacement = true
            when {
                merge != null -> onMerge(s.fromIndex, merge)
                s.targetIndex != s.fromIndex -> onMove(s.fromIndex, s.targetIndex)
            }
            withFrameNanos { }
            session = null
            overlayScale.snapTo(1f)
            withFrameNanos { }
            withFrameNanos { }
            suppressPlacement = false
        }
    }

    Box(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(RowSpacing),
            userScrollEnabled = session == null,
        ) {
            header()
            itemsIndexed(ranking.places, key = { _, p -> placeKey(p) }) { index, place ->
                val s = session
                val dragged = s?.key == placeKey(place)
                val shift by animateFloatAsState(
                    if (s == null || dragged) 0f else displacementOf(index, s),
                    if (s == null) androidx.compose.animation.core.snap() else OrdoMotion.smooth(0.5f),
                    label = "shift",
                )
                val highlight = s != null && s.mergeCandidate == index
                SortRow(
                    number = index + 1,
                    members = place.items.mapNotNull(items::get),
                    mergeTarget = highlight,
                    armed = highlight && s?.mergeArmed == true,
                    onDetach = onDetach,
                    dragModifier = Modifier.pointerInput(place) {
                        detectDragGestures(
                            onDragStart = { start(index) },
                            onDrag = { change, amount -> change.consume(); drag(amount.y) },
                            onDragEnd = { end() },
                            onDragCancel = { end() },
                        )
                    },
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = OrdoMotion.smooth(),
                            placementSpec = if (suppressPlacement) null else OrdoMotion.smooth(IntOffset.VisibilityThreshold),
                            fadeOutSpec = OrdoMotion.smooth(),
                        )
                        .graphicsLayer {
                            translationY = shift
                            alpha = if (dragged) 0f else 1f
                        }
                        .pointerInput(place) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { start(index) },
                                onDrag = { change, amount -> change.consume(); drag(amount.y) },
                                onDragEnd = { end() },
                                onDragCancel = { end() },
                            )
                        },
                )
            }
        }
        // Floating copy of the row being dragged.
        session?.let { s ->
            val place = ranking.places.getOrNull(s.fromIndex)
            if (place != null) {
                SortRow(
                    number = s.fromIndex + 1,
                    members = place.items.mapNotNull(items::get),
                    mergeTarget = false,
                    armed = false,
                    onDetach = {},
                    dragModifier = Modifier,
                    lifted = true,
                    modifier = Modifier
                        .padding(horizontal = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr))
                        // Item offsets exclude the list's top padding; the overlay box does not.
                        .offset { IntOffset(0, (overlayY.value - listState.layoutInfo.viewportStartOffset).roundToInt()) }
                        .graphicsLayer {
                            scaleX = overlayScale.value
                            scaleY = overlayScale.value
                        },
                )
            }
        }
    }
    LaunchedEffect(ranking) {
        // A new ranking from the database while not dragging needs no special handling;
        // this keeps any stale session from surviving a data change.
        if (session?.settling == false) session = null
    }
}

/** One row in sort mode. Tied entries each get a button to take them out of the tie. */
@Composable
private fun SortRow(
    number: Int,
    members: List<RankItem>,
    mergeTarget: Boolean,
    armed: Boolean,
    onDetach: (ItemId) -> Unit,
    dragModifier: Modifier,
    modifier: Modifier = Modifier,
    lifted: Boolean = false,
) {
    val ring by animateFloatAsState(if (armed) 1f else if (mergeTarget) 0.45f else 0f, OrdoMotion.snappy(), label = "ring")
    val grow by animateFloatAsState(if (armed) 1.025f else 1f, OrdoMotion.bouncy(), label = "grow")
    PillowSurface(
        modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = grow; scaleY = grow }
            .border(2.dp, OrdoPalette.Yellow.copy(alpha = ring), OrdoShapes.Row),
        shape = OrdoShapes.Row,
        elevation = if (lifted) 26.dp else 8.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                DotMatrixText(number.toString().padStart(2, '0'), 16.dp, color = if (armed) OrdoPalette.Yellow else Color.White)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                members.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ItemImage(item.image, item.title, if (members.size > 1) 34.dp else 42.dp)
                        Text(item.title, Modifier.weight(1f), style = OrdoTheme.type.itemTitle, color = OrdoTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (members.size > 1) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).clickable { onDetach(item.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(OrdoIcons.Split, stringResource(R.string.resolve_tie), Modifier.size(18.dp), tint = OrdoTheme.colors.textSecondary)
                            }
                        }
                    }
                }
            }
            Box(dragModifier.size(width = 44.dp, height = 52.dp), contentAlignment = Alignment.Center) {
                Icon(OrdoIcons.Grip, null, Modifier.size(22.dp), tint = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}
