package io.github.tombolinero.ordo.feature.item

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.data.CropRect
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ImageId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.ui.ImageCropDialog
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.OrdoTextField
import io.github.tombolinero.ordo.core.ui.R as UiR

@Composable
fun EditItemSheet(
    item: RankItem,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, pictures: List<PictureDraft>, cover: Int) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(item.title) }
    var note by rememberSaveable { mutableStateOf(item.note) }
    var pictures by remember { mutableStateOf(item.images.map<ImageId, PictureDraft> { PictureDraft.Stored(it) }) }
    var cover by remember { mutableIntStateOf(item.images.indexOf(item.image).coerceAtLeast(0)) }
    var cropping by rememberSaveable { mutableStateOf<Uri?>(null) }
    // One picture: crop it like before. Several at once: take them as they are.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) { uris ->
        when (uris.size) {
            0 -> Unit
            1 -> cropping = uris.single()
            else -> pictures = (pictures + uris.map { PictureDraft.Picked(it, CropRect.Full) }).distinctBy { it.key }
        }
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    OrdoSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.edit_title), style = OrdoTheme.type.screenTitle, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 18.dp))
        Row(Modifier.padding(start = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.pictures), style = OrdoTheme.type.label, color = OrdoTheme.colors.textSecondary)
            if (pictures.size > 1) Text(pictures.size.toString(), style = OrdoTheme.type.label, color = OrdoTheme.colors.accent)
        }
        PictureStrip(
            pictures, cover, title,
            onCover = { cover = it },
            onRemove = { i ->
                pictures = pictures.filterIndexed { j, _ -> j != i }
                cover = when {
                    i < cover -> cover - 1
                    i == cover -> 0
                    else -> cover
                }.coerceIn(0, (pictures.size - 1).coerceAtLeast(0))
            },
            onAdd = pick,
        )
        if (pictures.size > 1) {
            Text(stringResource(R.string.pictures_hint), style = OrdoTheme.type.meta, color = OrdoTheme.colors.textTertiary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
        Spacer(Modifier.height(18.dp))
        OrdoTextField(title, { title = it }, stringResource(R.string.title))
        Spacer(Modifier.height(18.dp))
        OrdoTextField(note, { note = it }, stringResource(R.string.note), singleLine = false, minHeight = 120.dp)
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            GlassPillButton(stringResource(UiR.string.action_cancel), onDismiss)
            GlassPillButton(
                stringResource(UiR.string.action_save),
                { onSave(title, note, pictures, cover) },
                icon = OrdoIcons.Check, accent = true, enabled = title.isNotBlank(),
            )
        }
    }
    cropping?.let { uri ->
        ImageCropDialog(uri, onDismiss = { cropping = null }) { rect ->
            // The first picture of an entry becomes its cover automatically.
            pictures = (pictures + PictureDraft.Picked(uri, rect)).distinctBy { it.key }
            cropping = null
        }
    }
}

private const val MAX_PICK = 20
