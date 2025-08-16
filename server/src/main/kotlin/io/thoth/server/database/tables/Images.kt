package io.thoth.server.database.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

object TImages : UUIDTable("Images") {
    val blob = blob("image")
}

class Image(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Image>(TImages)

    var blob by TImages.blob
}
