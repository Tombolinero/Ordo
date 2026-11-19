package io.github.tombolinero.ordo.feature.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.OrdoTextField
import io.github.tombolinero.ordo.core.ui.PictureDraft
import io.github.tombolinero.ordo.core.ui.PicturesEditor
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.rememberPicturesState

@Composable
fun EditItemSheet(
    item: RankItem,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, pictures: List<PictureDraft>, cover: Int) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(item.title) }
    var note by rememberSaveable { mutableStateOf(item.note) }
    val pictures = rememberPicturesState(item.images, item.image)

    OrdoSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.edit_title), style = OrdoTheme.type.screenTitle, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 18.dp))
        PicturesEditor(pictures, title)
        Spacer(Modifier.height(18.dp))
        OrdoTextField(title, { title = it }, stringResource(R.string.title))
        Spacer(Modifier.height(18.dp))
        OrdoTextField(note, { note = it }, stringResource(R.string.note), singleLine = false, minHeight = 120.dp)
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            GlassPillButton(stringResource(UiR.string.action_cancel), onDismiss)
            GlassPillButton(
                stringResource(UiR.string.action_save),
                { onSave(title, note, pictures.pictures, pictures.cover) },
                icon = OrdoIcons.Check, accent = true, enabled = title.isNotBlank(),
            )
        }
    }
}
