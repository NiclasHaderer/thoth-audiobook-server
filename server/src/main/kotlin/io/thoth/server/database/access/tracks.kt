package io.thoth.server.database.access

import io.thoth.server.database.tables.TrackEntity

fun TrackEntity.markAsTouched() {
    scanIndex = library.scanIndex
}

fun TrackEntity.hasBeenUpdated(fileModifiedAt: Long) = this.accessTime < fileModifiedAt
