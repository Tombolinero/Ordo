package io.github.tombolinero.ordo.core.ui

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.model.RankList

/** Create a list ([existing] == null) or edit title, description and card colour. */
@Composable
fun ListEditorSheet(
    existing: RankList?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, color: ListColor) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(existing?.description.orEmpty()) }
    var color by rememberSaveable { mutableStateOf(existing?.color ?: ListColor.Magenta) }
    val canSave = title.isNotBlank()
    OrdoSheet(onDismiss = onDismiss) {
        Text(
            stringResource(if (existing == null) R.string.editor_new else R.string.editor_edit),
            style = OrdoTheme.type.screenTitle,
            color = OrdoTheme.colors.textPrimary,
            modifier = Modifier.padding(start = 4.dp, bottom = 22.dp),
        )
        OrdoTextField(title, { title = it }, stringResource(R.string.editor_title), placeholder = stringResource(R.string.editor_title_hint))
        Spacer(Modifier.height(18.dp))
        OrdoTextField(
            description, { description = it }, stringResource(R.string.editor_description),
            placeholder = stringResource(R.string.editor_description_hint), imeAction = ImeAction.Done,
        )
        Spacer(Modifier.height(22.dp))
        Text(stringResource(R.string.editor_color), style = OrdoTheme.type.overline, color = OrdoTheme.colors.textTertiary, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ListColor.entries.forEach { c ->
                ColorSwatch(c, selected = c == color, onClick = { color = c })
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            GlassPillButton(stringResource(R.string.action_cancel), onDismiss)
            GlassPillButton(
                stringResource(if (existing == null) R.string.editor_create else R.string.action_save),
                onClick = { if (canSave) onSave(title, description, color) },
                icon = OrdoIcons.Check,
                accent = true,
                enabled = canSave,
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: ListColor, selected: Boolean, onClick: () -> Unit, size: Dp = 46.dp) {
    val haptics = LocalHapticFeedback.current
    val ring by animateDpAsState(if (selected) 2.dp else 0.dp, OrdoMotion.bouncy(Dp.VisibilityThreshold), label = "ring")
    val scale by animateFloatAsState(if (selected) 1f else 0.86f, OrdoMotion.bouncy(), label = "scale")
    val shape = SmoothCornerShape(16.dp)
    val label = stringResource(color.labelRes())
    Box(
        Modifier
            .size(size + 8.dp)
            .border(ring, if (selected) OrdoPalette.Yellow else Color.Transparent, SmoothCornerShape(20.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .semantics {
                this.selected = selected
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        PillowSurface(
            Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale },
            style = color.pillow(),
            shape = shape,
            elevation = 6.dp,
        ) {
            if (selected) Icon(OrdoIcons.Check, null, Modifier.align(Alignment.Center).size(18.dp), tint = Color.White)
        }
    }
}
