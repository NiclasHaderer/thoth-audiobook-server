package io.github.huiibuh.db.models

import io.github.huiibuh.db.models.Artists.uniqueIndex
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


object Tracks : IntIdTable() {
    val title = varchar("title", 255)
    val trackNr = integer("trackNr")
    val duration = integer("duration")
    val accessTime = long("accessTime")
    val path = text("path").uniqueIndex()
    val album = integer("album").references(Albums.id)
    val artist = integer("artist").references(Artists.id)
    val composer = integer("composer").references(Artists.id).nullable()
    val collection = integer("collection").references(Collections.id).nullable()
    val collectionIndex = integer("collectionIndex").nullable()
}


class Track(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Track>(Tracks)

    var title by Tracks.title
    var trackNr by Tracks.trackNr
    var duration by Tracks.duration
    var accessTime by Tracks.accessTime
    var path by Tracks.path
    var album by Album referencedOn Tracks.album
    var artist by Artist referencedOn Tracks.artist
    var composer by Artist optionalReferencedOn Tracks.composer
    var collection by Collection optionalReferencedOn Tracks.collection
    var collectionIndex by Tracks.collectionIndex
}
