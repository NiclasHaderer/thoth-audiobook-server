package io.github.huiibuh.db.tables

import io.github.huiibuh.models.TrackModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


object TTracks : UUIDTable("Tracks") {
    val title = varchar("title", 255)
    val duration = integer("duration")
    val accessTime = long("accessTime")
    val trackNr = integer("trackNr").nullable()
    val path = text("path").uniqueIndex()
    val book = reference("book", TBooks)
    val author = reference("author", TAuthors)
    val narrator = reference("narrator", TAuthors).nullable()
    val series = reference("series", TSeries).nullable()
    val seriesIndex = integer("seriesIndex").nullable()
    val scanIndex = integer("scanIndex")
}


class Track(id: EntityID<UUID>) : UUIDEntity(id), ToModel<TrackModel> {
    companion object : UUIDEntityClass<Track>(TTracks)

    private val bookID by TTracks.book
    private val authorID by TTracks.author
    private val narratorID by TTracks.narrator
    private val seriesID by TTracks.series

    var title by TTracks.title
    var trackNr by TTracks.trackNr
    var duration by TTracks.duration
    var accessTime by TTracks.accessTime
    var path by TTracks.path
    var book by Book referencedOn TTracks.book
    var author by Author referencedOn TTracks.author
    var narrator by Author optionalReferencedOn TTracks.narrator
    var series by Series optionalReferencedOn TTracks.series
    var seriesIndex by TTracks.seriesIndex
    var scanIndex by TTracks.scanIndex

    override fun toModel() = TrackModel(
        id = id.value,
        title = title,
        trackNr = trackNr,
        duration = duration,
        accessTime = accessTime,
        book = bookID.value,
        author = authorID.value,
        narrator = narratorID?.value,
        series = seriesID?.value,
        seriesIndex = seriesIndex
    )
}
