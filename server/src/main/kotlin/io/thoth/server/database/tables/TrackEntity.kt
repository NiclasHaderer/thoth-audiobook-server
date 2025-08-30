package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

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
    var book by BookEntity referencedOn TracksTable.book

    var scanIndex by TracksTable.scanIndex
}
