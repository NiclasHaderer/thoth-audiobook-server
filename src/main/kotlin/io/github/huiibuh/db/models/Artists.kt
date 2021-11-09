package io.github.huiibuh.db.models

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Artists : UUIDTable() {
    val name = varchar("name", 255).uniqueIndex()
    val description = text("description").nullable()
    val asin = text("asin").nullable()
    val image = blob("image").nullable()
}


class Artist(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Artist>(Artists)

    var name by Artists.name
    var description by Artists.description
    var asin by Artists.asin
    var image by Artists.image
}
