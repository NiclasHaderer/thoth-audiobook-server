package io.thoth.server.database.access

import io.thoth.models.TitledId
import io.thoth.models.TrackModel
import io.thoth.server.common.extensions.findOne
import io.thoth.server.database.tables.TTracks
import io.thoth.server.database.tables.Track
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString
import org.jetbrains.exposed.sql.SortOrder

fun Track.Companion.removeUntouched() {
    TODO("Not yet implemented")
}

fun Track.Companion.getById(uuid: UUID): TrackModel? {
    return Track.findById(uuid)?.toModel()
}

fun Track.Companion.getByPath(path: Path) = getByPath(path.absolutePathString())

fun Track.Companion.getByPath(path: String): Track? {
    return Track.findOne { TTracks.path like path }
}

fun Track.Companion.forBook(bookID: UUID, order: SortOrder = SortOrder.ASC): List<TrackModel> {
    return rawForBook(bookID, order).map { it.toModel() }
}

fun Track.Companion.rawForBook(bookID: UUID, order: SortOrder = SortOrder.ASC): List<Track> {
    return Track.find { TTracks.book eq bookID }.orderBy(TTracks.trackNr to order).toList()
}

fun Track.markAsTouched() {
    TODO("Not yet implemented")
}

fun Track.hasBeenUpdated(updateTime: Long) = this.accessTime >= updateTime

fun Track.toModel() =
    TrackModel(
        id = id.value,
        title = title,
        path = path,
        trackNr = trackNr,
        updateTime = updateTime,
        duration = duration,
        accessTime = accessTime,
        book = TitledId(book.id.value, book.title),
    )
