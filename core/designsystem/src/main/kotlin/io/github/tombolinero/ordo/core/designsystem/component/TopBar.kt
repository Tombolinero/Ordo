package io.github.tombolinero.ordo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme

/**
 * Header of the reference: round glass button left, centred title, round glass button right.
 * Pass `null` for a side to keep the title centred with an empty slot.
 */
@Composable
fun OrdoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: (@Composable RowScope.() -> Unit)? = null,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (navigation != null) navigation() else Spacer(Modifier.size(48.dp))
        Text(
            title,
            Modifier.weight(1f),
            style = OrdoTheme.type.screenTitle,
            color = OrdoTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (action != null) action() else Spacer(Modifier.size(48.dp))
    }
}

/** Screen root: pure black with the faint dot grid of the reference. */
@Composable
fun OrdoBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().dotGridBackground(), content = content)
}
