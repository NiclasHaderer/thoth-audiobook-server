package io.github.huiibuh.db.models

import io.github.huiibuh.db.models.Artists.uniqueIndex
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

object Collections : IntIdTable() {
    val name = varchar("name", 250).uniqueIndex()
    val artist = integer("artist").references(Artists.id)
}

class Collection(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Collection>(Collections)
    var name by Collections.name
    var artist by Artist referencedOn Collections.artist
}
