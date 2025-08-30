package io.thoth.server.database.access

import io.thoth.models.NamedId
import io.thoth.models.TitledId
import io.thoth.models.TrackModel
import io.thoth.server.common.extensions.findOne
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.database.tables.TracksTable

fun TrackEntity.Companion.getByPath(path: String): TrackEntity? = TrackEntity.findOne { TracksTable.path like path }

fun TrackEntity.markAsTouched() {
    scanIndex = library.scanIndex
}

fun TrackEntity.hasBeenUpdated(updateTime: Long) = this.accessTime >= updateTime

fun TrackEntity.toModel() =
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
