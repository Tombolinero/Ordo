package io.github.tombolinero.ordo.core.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.tombolinero.ordo.core.data.CropRect
import io.github.tombolinero.ordo.core.data.DataGraph
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ImageId

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
private fun PictureStrip(
    pictures: List<PictureDraft>,
    cover: Int,
    title: String,
    onCover: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)) {
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
                        Icon(OrdoIcons.Star, stringResource(R.string.picture_cover), Modifier.size(12.dp), tint = OrdoPalette.YellowInk)
                    }
                }
                Badge(Modifier.align(Alignment.TopEnd).clickable { onRemove(i) }, Color(0xFF2A2A2A)) {
                    Icon(OrdoIcons.Close, stringResource(R.string.picture_remove), Modifier.size(12.dp), tint = Color.White)
                }
            }
        }
        item(key = "add") {
            Box(
                Modifier.animateItem().padding(4.dp).size(76.dp).clip(SmoothCornerShape(22.dp))
                    .background(Color(0xFF1C1C1C)).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Icon(OrdoIcons.Plus, stringResource(R.string.picture_add), Modifier.size(24.dp), tint = OrdoTheme.colors.accent) }
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

/** Pictures being edited in a sheet (new entry or existing one). */
@Stable
class PicturesState(initial: List<PictureDraft>, initialCover: Int) {
    var pictures by mutableStateOf(initial)
        private set
    var cover by mutableIntStateOf(initialCover.coerceIn(0, (initial.size - 1).coerceAtLeast(0)))

    fun add(new: List<PictureDraft>) {
        pictures = (pictures + new).distinctBy { it.key }
    }

    fun remove(index: Int) {
        pictures = pictures.filterIndexed { j, _ -> j != index }
        cover = when {
            index < cover -> cover - 1
            index == cover -> 0
            else -> cover
        }.coerceIn(0, (pictures.size - 1).coerceAtLeast(0))
    }
}

@Composable
fun rememberPicturesState(images: List<ImageId> = emptyList(), cover: ImageId? = null): PicturesState =
    remember { PicturesState(images.map { PictureDraft.Stored(it) }, images.indexOf(cover).coerceAtLeast(0)) }

/**
 * "Pictures" section of the entry sheets. Picking one picture opens the crop dialog;
 * picking several at once adds them as they are. Tap = cover, cross = remove.
 */
@Composable
fun PicturesEditor(state: PicturesState, title: String, modifier: Modifier = Modifier) {
    var cropping by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) { uris ->
        when (uris.size) {
            0 -> Unit
            1 -> cropping = uris.single()
            else -> state.add(uris.map { PictureDraft.Picked(it, CropRect.Full) })
        }
    }
    Column(modifier) {
        Row(Modifier.padding(start = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.pictures), style = OrdoTheme.type.label, color = OrdoTheme.colors.textSecondary)
            if (state.pictures.size > 1) Text(state.pictures.size.toString(), style = OrdoTheme.type.label, color = OrdoTheme.colors.accent)
        }
        PictureStrip(
            state.pictures, state.cover, title,
            onCover = { state.cover = it },
            onRemove = state::remove,
            onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
        if (state.pictures.size > 1) {
            Text(stringResource(R.string.pictures_hint), style = OrdoTheme.type.meta, color = OrdoTheme.colors.textTertiary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }
    cropping?.let { uri ->
        ImageCropDialog(uri, onDismiss = { cropping = null }) { rect ->
            state.add(listOf(PictureDraft.Picked(uri, rect)))
            cropping = null
        }
    }
}

/** Stores freshly picked pictures. Returns the cover and all pictures in order. */
suspend fun DataGraph.importPictures(context: Context, pictures: List<PictureDraft>, cover: Int): Pair<ImageId?, List<ImageId>> {
    val ids = pictures.map { p ->
        when (p) {
            is PictureDraft.Stored -> p.id
            is PictureDraft.Picked -> runCatching { images.import(context, p.uri, p.crop) }.getOrNull()
        }
    }
    return (ids.getOrNull(cover) ?: ids.firstNotNullOfOrNull { it }) to ids.filterNotNull()
}

private const val MAX_PICK = 20
