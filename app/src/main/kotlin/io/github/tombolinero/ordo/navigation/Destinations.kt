package io.github.tombolinero.ordo.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import io.github.tombolinero.ordo.BuildConfig
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.feature.duel.DuelRoute
import io.github.tombolinero.ordo.feature.duel.RematchRoute
import io.github.tombolinero.ordo.feature.item.ItemRoute
import io.github.tombolinero.ordo.feature.ranking.RankingRoute
import io.github.tombolinero.ordo.feature.search.SearchRoute
import io.github.tombolinero.ordo.feature.settings.SettingsRoute
import io.github.tombolinero.ordo.feature.settings.TrashRoute

/** All screens except home; each feature only knows its own callbacks. */
fun NavGraphBuilder.destinations(nav: NavHostController) {
    fun back() {
        nav.popBackStack()
    }

    screen<ListDestination> { entry ->
        val listId = ListId(entry.toRoute<ListDestination>().listId)
        RankingRoute(
            listId = listId,
            onBack = ::back,
            onOpenItem = { nav.navigate(ItemDestination(listId.value, it.value)) },
            onPlace = { nav.navigate(DuelDestination(listId.value, it.value)) },
            onOpenSettings = { nav.navigate(SettingsDestination) },
            onRematch = { nav.navigate(RematchDestination(listId.value)) },
        )
    }
    screen<ItemDestination> { entry ->
        val route = entry.toRoute<ItemDestination>()
        ItemRoute(
            listId = ListId(route.listId),
            itemId = ItemId(route.itemId),
            onBack = ::back,
            onPlace = { nav.navigate(DuelDestination(route.listId, it.value)) },
        )
    }
    screen<DuelDestination> { entry ->
        val route = entry.toRoute<DuelDestination>()
        DuelRoute(ListId(route.listId), ItemId(route.itemId), onClose = ::back)
    }
    screen<RematchDestination> { entry ->
        RematchRoute(ListId(entry.toRoute<RematchDestination>().listId), onClose = ::back)
    }
    screen<SearchDestination> {
        SearchRoute(onBack = ::back, onOpenItem = { hit -> nav.navigate(ItemDestination(hit.list.id.value, hit.item.id.value)) })
    }
    screen<SettingsDestination> {
        SettingsRoute(versionName = BuildConfig.VERSION_NAME, onBack = ::back, onOpenTrash = { nav.navigate(TrashDestination) })
    }
    screen<TrashDestination> {
        TrashRoute(onBack = ::back)
    }
}
