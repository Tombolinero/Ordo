package io.github.tombolinero.ordo.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme

/** Bottom sheet in the Ordo style: graphite surface, big top radius, soft scrim. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdoSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        containerColor = Color(0xFF161616),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .drawBehind { drawRect(Color.White.copy(alpha = 0.25f)) },
            )
        },
    ) {
        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 22.dp).padding(bottom = 20.dp),
            content = content,
        )
    }
}

data class SheetAction(
    val icon: ImageVector,
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/** A titled list of actions, e.g. the long-press menu of a list card. */
@Composable
fun ColumnScope.ActionList(title: String?, actions: List<SheetAction>) {
    if (title != null) {
        Text(title, style = OrdoTheme.type.sectionTitle, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(bottom = 10.dp, start = 4.dp))
    }
    actions.forEach { a ->
        val tint = if (a.destructive) OrdoTheme.colors.danger else OrdoTheme.colors.textPrimary
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = a.onClick)
                .padding(horizontal = 6.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(a.icon, null, Modifier.size(22.dp), tint = tint)
            Text(a.label, style = OrdoTheme.type.body, color = tint)
        }
    }
    Spacer(Modifier.height(4.dp))
}
