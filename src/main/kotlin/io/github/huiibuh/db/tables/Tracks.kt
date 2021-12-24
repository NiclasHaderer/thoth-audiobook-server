package io.github.huiibuh.db.tables

import io.github.huiibuh.models.TitledId
import io.github.huiibuh.models.TrackModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


object TTracks : UUIDTable("Tracks") {
    val title = varchar("title", 255)
    val trackNr = integer("trackNr").nullable()
    val duration = integer("duration")
    val accessTime = long("accessTime")
    val path = text("path").uniqueIndex()
    val book = reference("book", TBooks)
    val scanIndex = integer("scanIndex")
}


class Track(id: EntityID<UUID>) : UUIDEntity(id), ToModel<TrackModel> {
    companion object : UUIDEntityClass<Track>(TTracks)

    var title by TTracks.title
    var trackNr by TTracks.trackNr
    var duration by TTracks.duration
    var accessTime by TTracks.accessTime
    var path by TTracks.path
    var book by Book referencedOn TTracks.book

    var scanIndex by TTracks.scanIndex

    override fun toModel() = TrackModel(
        id = id.value,
        title = title,
        trackNr = trackNr,
        duration = duration,
        accessTime = accessTime,
        book = TitledId(
            title = book.title,
            id = book.id.value
        ),
    )
}
