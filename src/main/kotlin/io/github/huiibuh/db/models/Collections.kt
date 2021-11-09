package io.github.huiibuh.db.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Collections : IntIdTable() {
    val name = varchar("name", 250).uniqueIndex()
    val artist = reference("artist", Artists)
}

class Collection(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Collection>(Collections)

    var name by Collections.name
    var artist by Artist referencedOn Collections.artist
}
