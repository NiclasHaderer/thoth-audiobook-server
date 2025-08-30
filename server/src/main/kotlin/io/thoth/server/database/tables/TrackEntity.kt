package io.thoth.server.database.tables

import io.thoth.models.NamedId
import io.thoth.models.TitledId
import io.thoth.models.Track
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class TrackEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TrackEntity>(TracksTable)

    var title by TracksTable.title
    var trackNr by TracksTable.trackNr
    var updateTime by TracksTable.updateTime
    var duration by TracksTable.duration
    var accessTime by TracksTable.accessTime
    var path by TracksTable.path
    var scanIndex by TracksTable.scanIndex
    var book by BookEntity referencedOn TracksTable.book
    var library by LibraryEntity referencedOn TracksTable.library

    fun toModel() =
        Track(
            id = id.value,
            title = title,
            path = path,
            trackNr = trackNr,
            updateTime = updateTime,
            duration = duration,
            accessTime = accessTime,
            book = TitledId(book.id.value, book.title),
            library = NamedId(book.library.id.value, book.library.name),
        )
}
