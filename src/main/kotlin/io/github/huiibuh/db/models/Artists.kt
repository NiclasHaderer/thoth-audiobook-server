package io.github.huiibuh.db.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Artists : IntIdTable() {
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val asin = text("asin").nullable()
    val image = blob("image").nullable()
    //    val albums = TODO()
    //    val collections = TODO()
}


class Artist(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Artist>(Artists)

    val name by Artists.name
    val description by Artists.description
    val asin by Artists.asin
    val image by Artists.image
}
