package io.github.tombolinero.ordo.feature.ranking

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton

import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.component.dotGridBackground
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.RankedList
import io.github.tombolinero.ordo.core.ui.ItemImage
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.pillow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val SHARE_LIMIT = 10

/** Preview of the share picture plus a "Share" button that hands a PNG to any app. */
@Composable
fun ShareRankingSheet(list: RankedList, onDismiss: () -> Unit) {
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.share_title)
    OrdoSheet(onDismiss = onDismiss) {
        Text(chooserTitle, style = OrdoTheme.type.screenTitle, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ShareCard(
                list,
                Modifier.drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            GlassPillButton(stringResource(UiR.string.action_cancel), onDismiss)
            GlassPillButton(stringResource(R.string.share_button), {
                scope.launch {
                    shareLayer(context, layer, list.list.title, chooserTitle)
                    onDismiss()
                }
            }, icon = OrdoIcons.Share, accent = true)
        }
    }
}

private suspend fun shareLayer(context: Context, layer: GraphicsLayer, title: String, chooserTitle: String) {
    val bitmap = layer.toImageBitmap().asAndroidBitmap()
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val safe = title.replace(Regex("[^A-Za-z0-9ÄÖÜäöüß _-]"), "").trim().ifEmpty { "Ordo" }
        File(dir, "$safe.png").also { f -> f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".share", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}

/** The picture that gets shared: list header in its colour and the top ten. */
@Composable
internal fun ShareCard(list: RankedList, modifier: Modifier = Modifier) {
    val places = list.ranking.places.take(SHARE_LIMIT)
    val style = list.list.color.pillow()
    Column(
        modifier
            .width(340.dp)
            .clip(SmoothCornerShape(34.dp))
            .dotGridBackground()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PillowSurface(Modifier.fillMaxWidth(), style = style, shape = OrdoShapes.CardSmall) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(list.list.title, style = OrdoTheme.type.cardTitle, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.share_top, places.size), style = OrdoTheme.type.label, color = Color.White.copy(alpha = 0.7f))
                }
                DotMatrixText(places.size.toString().padStart(2, '0'), 24.dp)
            }
        }
        places.forEachIndexed { index, place ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                    DotMatrixText((index + 1).toString().padStart(2, '0'), 15.dp, color = if (index == 0) OrdoPalette.Yellow else Color.White)
                }
                val first = list.item(place.items.first())
                ItemImage(first.image, first.title, 36.dp)
                Text(
                    place.items.joinToString(" = ") { list.item(it).title },
                    Modifier.weight(1f),
                    style = OrdoTheme.type.itemTitle,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            stringResource(R.string.share_footer),
            style = OrdoTheme.type.meta,
            color = OrdoTheme.colors.textTertiary,
            modifier = Modifier.align(Alignment.End).padding(top = 6.dp, end = 6.dp),
        )
    }
}

