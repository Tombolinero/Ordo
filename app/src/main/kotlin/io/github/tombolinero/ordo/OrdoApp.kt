package io.github.tombolinero.ordo

import android.app.Application
import io.github.tombolinero.ordo.core.data.DataGraph
import io.github.tombolinero.ordo.core.ui.OrdoAppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OrdoApp : Application(), OrdoAppGraph {
    override val data: DataGraph by lazy {
        DataGraph(this, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    /** Lives as long as the process; for housekeeping that must not block the UI. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { data.repository.purgeExpired() }
    }
}
