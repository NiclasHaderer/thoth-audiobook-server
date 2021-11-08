package io.github.huiibuh.db.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Artists : IntIdTable() {
    val name = varchar("name", 255).uniqueIndex()
    val description = text("description").nullable()
    val asin = text("asin").nullable()
    val image = blob("image").nullable()
}


class Artist(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Artist>(Artists)

    var name by Artists.name
    var description by Artists.description
    var asin by Artists.asin
    var image by Artists.image
}
