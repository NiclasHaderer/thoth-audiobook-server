package io.github.huiibuh.db.models

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


object Tracks : UUIDTable() {
    val title = varchar("title", 255)
    val duration = integer("duration")
    val accessTime = long("accessTime")
    val trackNr = integer("trackNr").nullable()
    val path = text("path").uniqueIndex()
    val album = reference("album", Albums)
    val artist = reference("artist", Artists)
    val composer = reference("composer", Artists).nullable()
    val collection = reference("collection", Collections).nullable()
    val collectionIndex = integer("collectionIndex").nullable()
}


class Track(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Track>(Tracks)

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
