package io.github.huiibuh.db.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

object Collections : IntIdTable() {
    val name = varchar("name", 250)
    val artist = integer("artist").references(Artists.id)
//    val albums = TODO()
}

class Collection(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Collection>(Collections)
    val name by Collections.name
    val artist by Artist referencedOn Collections.artist
}

object CollectionArtists : Table() {
    val collectionID = integer("artistID").references(Collections.id)
    val albumID = integer("albumID").references(Albums.id)
    override val primaryKey = PrimaryKey(collectionID, albumID, name = "artist_album")
}
