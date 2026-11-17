package io.github.tombolinero.ordo.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.tombolinero.ordo.MainActivity
import io.github.tombolinero.ordo.R
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.ui.dataGraph
import kotlinx.coroutines.flow.first

/** Home screen widget: the top three of one list (chosen when the widget is added). */
class TopWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    data class Top(val listId: String, val title: String, val count: Int, val rows: List<Pair<Int, String>>)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val listId = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, id)[LIST_KEY]
        val top = listId?.let { load(context, it) }
        provideContent { Content(context, top) }
    }

    private suspend fun load(context: Context, listId: String): Top? {
        val list = context.dataGraph.repository.observeRankedList(ListId(listId)).first() ?: return null
        val rows = list.ranking.places.take(3).mapIndexed { i, p -> (i + 1) to p.items.joinToString(" = ") { list.item(it).title } }
        return Top(listId, list.list.title, list.items.size, rows)
    }

    @Composable
    private fun Content(context: Context, top: Top?) {
        val open = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            top?.let { putExtra(MainActivity.EXTRA_LIST_ID, it.listId) }
        }
        Column(
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(28.dp)
                .background(ColorProvider(Color(0xFF111111)))
                .padding(16.dp)
                .clickable(actionStartActivity(open)),
        ) {
            if (top == null) {
                Text(context.getString(R.string.widget_empty), style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp))
                return@Column
            }
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    top.title,
                    GlanceModifier.defaultWeight(),
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
                Image(ImageProvider(dotNumberBitmap(top.count.coerceAtMost(99).toString().padStart(2, '0'), 36, 0x99FFFFFF.toInt())), null)
            }
            Spacer(GlanceModifier.height(10.dp))
            top.rows.forEach { (place, title) ->
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        ImageProvider(dotNumberBitmap(place.toString().padStart(2, '0'), 42, if (place == 1) 0xFFF6D818.toInt() else 0xFFFFFFFF.toInt())),
                        null,
                    )
                    Spacer(GlanceModifier.width(12.dp))
                    Text(title, style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp), maxLines = 1)
                }
            }
        }
    }

    companion object {
        val LIST_KEY = stringPreferencesKey("list_id")

        /** Refreshes every placed widget (called when lists change). */
        suspend fun refresh(context: Context) {
            if (GlanceAppWidgetManager(context).getGlanceIds(TopWidget::class.java).isNotEmpty()) TopWidget().updateAll(context)
        }
    }
}

class TopWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopWidget()
}
