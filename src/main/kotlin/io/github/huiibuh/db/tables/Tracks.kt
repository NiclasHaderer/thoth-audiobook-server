package io.github.huiibuh.db.tables

import io.github.huiibuh.models.NamedId
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
    val cover = reference("cover", TImages).nullable()
    val duration = integer("duration")
    val accessTime = long("accessTime")
    val path = text("path").uniqueIndex()
    val book = reference("book", TBooks)
    val author = reference("author", TAuthors)
    val narrator = varchar("name", 255).nullable()
    val series = reference("series", TSeries).nullable()
    val seriesIndex = float("seriesIndex").nullable()
    val scanIndex = integer("scanIndex")
}


class Track(id: EntityID<UUID>) : UUIDEntity(id), ToModel<TrackModel> {
    companion object : UUIDEntityClass<Track>(TTracks)

    private val coverID by TTracks.cover

    var title by TTracks.title
    var trackNr by TTracks.trackNr
    var cover by Image optionalReferencedOn TTracks.cover
    var duration by TTracks.duration
    var accessTime by TTracks.accessTime
    var path by TTracks.path
    var book by Book referencedOn TTracks.book
    var author by Author referencedOn TTracks.author
    var narrator by TTracks.narrator
    var series by Series optionalReferencedOn TTracks.series
    var seriesIndex by TTracks.seriesIndex
    var scanIndex by TTracks.scanIndex

    override fun toModel() = TrackModel(
        id = id.value,
        title = title,
        cover = coverID?.value,
        trackNr = trackNr,
        duration = duration,
        accessTime = accessTime,
        book = TitledId(
            title = book.title,
            id = book.id.value
        ),
        author = NamedId(
            name = author.name,
            id = author.id.value
        ),
        narrator = narrator,
        series = if (series != null) TitledId(
            title = series!!.title,
            id = series!!.id.value
        ) else null,
        seriesIndex = seriesIndex
    )
}
