package io.github.huiibuh.db.models

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


object Albums : UUIDTable() {
    val name = varchar("name", 255)
    val artist = reference("artist", Artists)
    val composer = reference("composer", Artists).nullable()
    val collection = reference("collection", Collections).nullable()
    val collectionIndex = integer("collectionIndex").nullable()
    val cover = blob("cover").nullable()
}

class Album(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Album>(Albums)

    var name by Albums.name
    var artist by Artist referencedOn Albums.artist
    var composer by Artist optionalReferencedOn Albums.composer
    var collection by Collection optionalReferencedOn Albums.collection
    var collectionIndex by Albums.collectionIndex
    var cover by Albums.cover
}
