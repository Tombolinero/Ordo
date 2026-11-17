package io.github.tombolinero.ordo.core.data

import io.github.tombolinero.ordo.core.database.ItemEntity
import io.github.tombolinero.ordo.core.database.ListEntity
import io.github.tombolinero.ordo.core.model.ImageId
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankList
import io.github.tombolinero.ordo.core.model.Ranking

internal fun ListEntity.toModel() = RankList(
    id = ListId(id), title = title, description = description, color = ListColor.fromKey(color),
    pinned = pinned, sortOrder = sortOrder, createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt,
)

internal fun RankList.toEntity() = ListEntity(
    id = id.value, title = title, description = description, color = color.key, pinned = pinned,
    sortOrder = sortOrder, createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt,
)

internal fun ItemEntity.toModel() = RankItem(
    id = ItemId(id), listId = ListId(listId), title = title, note = note, image = imageId?.let(::ImageId),
    createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt,
)

internal fun List<ItemEntity>.toRanking(): Ranking =
    Ranking.fromPositions(map { Ranking.Position(ItemId(it.id), it.place, it.placeOrder) })
