package io.github.huiibuh.db.models

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Collections : UUIDTable() {
    val name = varchar("name", 250).uniqueIndex()
    val artist = reference("artist", Artists)
}

class Collection(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Collection>(Collections)

    var name by Collections.name
    var artist by Artist referencedOn Collections.artist
}
