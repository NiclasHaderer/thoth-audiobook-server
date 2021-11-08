package io.github.huiibuh.db.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


object Albums : IntIdTable() {
    val name = varchar("name", 255)
    val artist = integer("artist").references(Artists.id)
    val composer = integer("composer").references(Artists.id).nullable()
    val collection = integer("collection").references(Collections.id).nullable()
    val collectionIndex = integer("collectionIndex").nullable()
    val cover = blob("cover").nullable()
//    val tracks = TODO()
}

class Album(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Album>(Albums)

    val collectionIndex by Albums.collectionIndex
    val cover by Albums.cover
    val artist by Artist referencedOn Albums.artist
    val composer by Artist optionalReferencedOn Albums.composer
    val collection by Collection optionalBackReferencedOn Albums.collection
}
