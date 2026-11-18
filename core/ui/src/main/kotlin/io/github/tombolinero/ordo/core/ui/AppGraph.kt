package io.github.tombolinero.ordo.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.tombolinero.ordo.core.data.DataGraph

/** Implemented by the Application: gives screens access to the data layer without a DI framework. */
interface OrdoAppGraph {
    val data: DataGraph
}

val Context.dataGraph: DataGraph get() = (applicationContext as OrdoAppGraph).data

/** `val vm = ordoViewModel { data -> MyViewModel(data.repository) }` */
@Composable
inline fun <reified VM : ViewModel> ordoViewModel(key: String? = null, crossinline create: (DataGraph) -> VM): VM {
    val graph = LocalContext.current.dataGraph
    return viewModel(key = key, factory = viewModelFactory { initializer { create(graph) } })
}
