package io.thoth.database.access

import io.thoth.common.extensions.findOne
import io.thoth.database.tables.KeyValueSettings
import io.thoth.database.tables.TTracks
import io.thoth.database.tables.Track
import io.thoth.models.TitledId
import io.thoth.models.TrackModel
import org.jetbrains.exposed.sql.SortOrder
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString

fun Track.Companion.removeUntouched() {
    val kvSettings = KeyValueSettings.get()
    Track.find { TTracks.scanIndex eq kvSettings.scanIndex }.forEach {
        it.delete()
    }
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
    val kvSettings = KeyValueSettings.get()
    this.scanIndex = kvSettings.scanIndex + 1
}

fun Track.hasBeenUpdated(updateTime: Long) = this.accessTime >= updateTime

fun Track.toModel() = TrackModel(
    id = id.value,
    title = title,
    path = path,
    trackNr = trackNr,
    updateTime = updateTime,
    duration = duration,
    accessTime = accessTime,
    book = TitledId(
        title = book.title, id = book.id.value
    ),
)