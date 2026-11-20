package io.github.tombolinero.ordo.feature.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.OrdoTextField
import io.github.tombolinero.ordo.core.ui.PictureDraft
import io.github.tombolinero.ordo.core.ui.PicturesEditor
import io.github.tombolinero.ordo.core.ui.PicturesState
import io.github.tombolinero.ordo.core.ui.rememberPicturesState

/**
 * New entry: title, optional pictures (one is the cover), optional note.
 * "Place now" starts the duel, "Place later" puts it into "Not placed yet".
 */
@Composable
fun AddItemSheet(
    onDismiss: () -> Unit,
    pictures: PicturesState = rememberPicturesState(),
    onSave: (title: String, note: String, pictures: List<PictureDraft>, cover: Int, placeNow: Boolean) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    OrdoSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.add_title), style = OrdoTheme.type.screenTitle, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 18.dp))
        PicturesEditor(pictures, title)
        Spacer(Modifier.height(18.dp))
        OrdoTextField(title, { title = it }, stringResource(R.string.field_title), placeholder = stringResource(R.string.field_title_hint))
        Spacer(Modifier.height(18.dp))
        OrdoTextField(note, { note = it }, stringResource(R.string.field_note), placeholder = stringResource(R.string.field_note_hint), singleLine = false, minHeight = 110.dp)
        Spacer(Modifier.height(26.dp))
        // Stacked, full width: both labels always fit, the main action sits on top.
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassPillButton(stringResource(R.string.save_duel), { onSave(title, note, pictures.pictures, pictures.cover, true) }, Modifier.fillMaxWidth(), icon = OrdoIcons.Duel, accent = true, enabled = title.isNotBlank())
            GlassPillButton(stringResource(R.string.save_inbox), { onSave(title, note, pictures.pictures, pictures.cover, false) }, Modifier.fillMaxWidth(), icon = OrdoIcons.Inbox, enabled = title.isNotBlank())
        }
    }
}
