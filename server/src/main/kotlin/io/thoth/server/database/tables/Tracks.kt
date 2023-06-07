package io.thoth.server.database.tables

import java.time.LocalDateTime
import java.util.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime

// TODO make sure that two libraries do not cover the same paths, otherwise the path reference will
// not be unique
object TTracks : UUIDTable("Tracks") {
    val title = varchar("title", 255)
    val duration = integer("duration")
    val accessTime = long("accessTime")
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val path = text("path").uniqueIndex()
    val book = reference("book", TBooks)
    val scanIndex = ulong("scanIndex")
    val trackNr = integer("trackNr").nullable()
}

class Track(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Track>(TTracks)

    val library: Library
        get() = this.book.library

    var title by TTracks.title
    var trackNr by TTracks.trackNr
    var updateTime by TTracks.updateTime
    var duration by TTracks.duration
    var accessTime by TTracks.accessTime
    var path by TTracks.path
    var book by Book referencedOn TTracks.book

    var scanIndex by TTracks.scanIndex
}
