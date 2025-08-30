package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

// TODO make sure that two libraries do not cover the same paths, otherwise the path reference will
// not be unique
object TracksTable : UUIDTable("Tracks") {
    val title = varchar("title", 255)
    val duration = integer("duration")
    val accessTime = long("accessTime")
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val path = text("path").uniqueIndex()
    val book = reference("book", BooksTable)
    val scanIndex = ulong("scanIndex")
    val trackNr = integer("trackNr").nullable()
}

class TrackEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TrackEntity>(TracksTable)

    val library: LibraryEntity
        get() = this.book.library

    var title by TracksTable.title
    var trackNr by TracksTable.trackNr
    var updateTime by TracksTable.updateTime
    var duration by TracksTable.duration
    var accessTime by TracksTable.accessTime
    var path by TracksTable.path
    var book by BookeEntity referencedOn TracksTable.book

    var scanIndex by TracksTable.scanIndex
}
