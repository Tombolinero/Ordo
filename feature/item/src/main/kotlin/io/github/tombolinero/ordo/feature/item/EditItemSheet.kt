package io.github.tombolinero.ordo.feature.item

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.tombolinero.ordo.core.data.CropRect
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.ui.ImageCropDialog
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.OrdoTextField
import io.github.tombolinero.ordo.core.ui.R as UiR

@Composable
fun EditItemSheet(
    item: RankItem,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, image: Uri?, crop: CropRect?, removeImage: Boolean) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(item.title) }
    var note by rememberSaveable { mutableStateOf(item.note) }
    var picked by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cropping by rememberSaveable { mutableStateOf<Uri?>(null) }
    var crop by remember { mutableStateOf<CropRect?>(null) }
    var removed by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) cropping = uri }

    OrdoSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.edit_title), style = OrdoTheme.type.screenTitle, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 22.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val shape = SmoothCornerShape(22.dp)
            val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            when {
                picked != null -> AsyncImage(picked, null, Modifier.size(84.dp).clip(shape).clickable { pick() }, contentScale = ContentScale.Crop)
                else -> ItemImage(if (removed) null else item.image, title, 84.dp, Modifier.clickable { pick() }, shape = shape)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val hasImage = picked != null || (!removed && item.image != null)
                Text(
                    stringResource(if (hasImage) R.string.change_picture else R.string.add_picture),
                    style = OrdoTheme.type.label, color = OrdoTheme.colors.accent,
                    modifier = Modifier.clickable { pick() },
                )
                if (hasImage) {
                    Text(
                        stringResource(R.string.remove_picture),
                        style = OrdoTheme.type.label, color = OrdoTheme.colors.textSecondary,
                        modifier = Modifier.clickable { picked = null; crop = null; removed = true },
                    )
                }
            }
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
                { onSave(title, note, picked, crop, removed && picked == null) },
                icon = OrdoIcons.Check, accent = true, enabled = title.isNotBlank(),
            )
        }
    }
    cropping?.let { uri ->
        ImageCropDialog(uri, onDismiss = { cropping = null }) { rect ->
            picked = uri; crop = rect; cropping = null; removed = false
        }
    }
}
