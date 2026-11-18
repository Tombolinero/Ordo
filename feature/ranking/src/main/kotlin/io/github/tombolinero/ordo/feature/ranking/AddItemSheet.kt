package io.github.tombolinero.ordo.feature.ranking

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.tombolinero.ordo.core.data.CropRect
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.ui.ImageCropDialog
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.OrdoTextField

/**
 * New entry: title, optional picture (picked, then cropped), optional note.
 * "Place now" starts the duel, "Place later" puts it into "Not placed yet".
 */
@Composable
fun AddItemSheet(
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, image: Uri?, crop: CropRect?, placeNow: Boolean) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cropping by rememberSaveable { mutableStateOf<Uri?>(null) }
    var crop by remember { mutableStateOf<CropRect?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) cropping = uri }

    OrdoSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.add_title), style = OrdoTheme.type.screenTitle, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 22.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ImageSlot(picked) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            OrdoTextField(title, { title = it }, stringResource(R.string.field_title), Modifier.weight(1f), placeholder = stringResource(R.string.field_title_hint))
        }
        if (picked != null) {
            Text(
                stringResource(R.string.remove_image),
                style = OrdoTheme.type.label,
                color = OrdoTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp).clickable { picked = null; crop = null },
            )
        }
        Spacer(Modifier.height(18.dp))
        OrdoTextField(note, { note = it }, stringResource(R.string.field_note), placeholder = stringResource(R.string.field_note_hint), singleLine = false, minHeight = 110.dp)
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            GlassPillButton(stringResource(R.string.save_inbox), { onSave(title, note, picked, crop, false) }, icon = OrdoIcons.Inbox, enabled = title.isNotBlank())
            GlassPillButton(stringResource(R.string.save_duel), { onSave(title, note, picked, crop, true) }, icon = OrdoIcons.Duel, accent = true, enabled = title.isNotBlank())
        }
    }
    cropping?.let { uri ->
        ImageCropDialog(uri, onDismiss = { cropping = null }) { rect ->
            picked = uri
            crop = rect
            cropping = null
        }
    }
}

@Composable
private fun ImageSlot(uri: Uri?, onPick: () -> Unit) {
    val shape = SmoothCornerShape(20.dp)
    Box(
        Modifier
            .size(84.dp)
            .clip(shape)
            .drawBehind { drawRect(Color(0xFF0B0B0C)) }
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (uri == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(OrdoIcons.Image, null, Modifier.size(24.dp), tint = OrdoTheme.colors.textSecondary)
                Text(stringResource(R.string.pick_image), style = OrdoTheme.type.meta, color = OrdoTheme.colors.textTertiary, modifier = Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp), maxLines = 2)
            }
        } else {
            // Preview shows the whole picture; the crop is applied when saving.
            AsyncImage(model = uri, contentDescription = stringResource(R.string.change_image), contentScale = ContentScale.Crop, modifier = Modifier.size(84.dp))
        }
    }
}
