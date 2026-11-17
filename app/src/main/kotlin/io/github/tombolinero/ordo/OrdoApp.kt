package io.github.tombolinero.ordo

import android.app.Application
import io.github.tombolinero.ordo.core.data.DataGraph
import io.github.tombolinero.ordo.core.ui.OrdoAppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import io.github.tombolinero.ordo.widget.TopWidget

class OrdoApp : Application(), OrdoAppGraph {
    override val data: DataGraph by lazy {
        DataGraph(this, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    /** Lives as long as the process; for housekeeping that must not block the UI. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { data.repository.purgeExpired() }
        // Keep home screen widgets in sync with the lists.
        appScope.launch {
            @OptIn(FlowPreview::class)
            data.repository.observeSummaries().drop(1).debounce(800).collect { TopWidget.refresh(this@OrdoApp) }
        }
    }
}
