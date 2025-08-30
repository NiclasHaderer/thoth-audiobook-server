package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

object ImageTable : UUIDTable("Images") {
    val blob = blob("image")
}

class ImageEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ImageEntity>(ImageTable)

    var blob by ImageTable.blob
}
