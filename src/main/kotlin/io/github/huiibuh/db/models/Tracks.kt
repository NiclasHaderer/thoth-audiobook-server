package io.github.huiibuh.db.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime


object Tracks : IntIdTable() {
    val title = varchar("title", 255)
    val trackNr = integer("trackNr")
    val duration = float("duration")
    val accessTime = datetime("accessTime")
    val path = text("path")
    val album = integer("album").references(Albums.id)
    val artist = integer("artist").references(Artists.id)
    val composer = integer("composer").references(Artists.id).nullable()
    val collection = integer("collection").references(Collections.id).nullable()
    val collectionIndex = integer("collectionIndex").nullable()
}


class Track(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Track>(Tracks)
    val title by Tracks.title
    val trackNr by Tracks.trackNr
    val duration by Tracks.duration
    val accessTime by Tracks.accessTime
    val path by Tracks.path
    val album by Album referencedOn Tracks.album
    val artist by Artist referencedOn Tracks.artist
    val composer by Artist optionalReferencedOn Tracks.composer
    val collection by Collection optionalReferencedOn Tracks.collection
    val collectionIndex by Tracks.collectionIndex
}
