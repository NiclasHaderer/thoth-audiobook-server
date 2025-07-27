package io.thoth.server.database.access

import io.thoth.models.NamedId
import io.thoth.models.TitledId
import io.thoth.models.TrackModel
import io.thoth.server.common.extensions.findOne
import io.thoth.server.database.tables.TTracks
import io.thoth.server.database.tables.Track

fun Track.Companion.getByPath(path: String): Track? {
    return Track.findOne { TTracks.path like path }
}

fun Track.markAsTouched() {
    scanIndex = library.scanIndex
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
        library = NamedId(library.id.value, library.name),
    )
