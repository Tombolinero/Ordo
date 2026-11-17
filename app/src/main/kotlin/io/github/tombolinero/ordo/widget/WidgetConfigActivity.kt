package io.github.tombolinero.ordo.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import io.github.tombolinero.ordo.R
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.OrdoBackground
import io.github.tombolinero.ordo.core.designsystem.component.OrdoTopBar
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.ui.EmptyState
import io.github.tombolinero.ordo.core.ui.dataGraph
import io.github.tombolinero.ordo.core.ui.pillow
import kotlinx.coroutines.launch

/** Shown when the widget is placed: pick the list whose top three it should show. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val summaries = dataGraph.repository.observeSummaries()
        setContent {
            val lists by summaries.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            OrdoTheme {
                OrdoBackground {
                    Column(Modifier.fillMaxSize()) {
                        OrdoTopBar(stringResource(R.string.widget_pick))
                        val items = lists
                        if (items != null && items.isEmpty()) {
                            EmptyState(stringResource(R.string.widget_no_lists), stringResource(R.string.widget_no_lists_hint))
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(items.orEmpty(), key = { it.list.id.value }) { s ->
                                PillowSurface(Modifier.fillMaxWidth(), style = s.list.color.pillow()) {
                                    Row(
                                        Modifier.fillMaxWidth().clickable {
                                            scope.launch {
                                                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                                                updateAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId) {
                                                    it.toMutablePreferences().apply { this[TopWidget.LIST_KEY] = s.list.id.value }
                                                }
                                                TopWidget().update(this@WidgetConfigActivity, glanceId)
                                                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                                finish()
                                            }
                                        }.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(s.list.title, Modifier.weight(1f), style = OrdoTheme.type.cardTitle, color = OrdoTheme.colors.textPrimary)
                                        DotMatrixText(s.itemCount.coerceAtMost(99).toString().padStart(2, '0'), 22.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
