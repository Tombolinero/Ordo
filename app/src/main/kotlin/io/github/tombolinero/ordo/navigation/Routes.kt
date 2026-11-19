package io.github.tombolinero.ordo.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeDestination

@Serializable data class ListDestination(val listId: String)

@Serializable data class ItemDestination(val listId: String, val itemId: String)

@Serializable data class DuelDestination(val listId: String, val itemId: String)

@Serializable data object SearchDestination

@Serializable data object SettingsDestination

@Serializable data object TrashDestination

@Serializable data class RematchDestination(val listId: String)
