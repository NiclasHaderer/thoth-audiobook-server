package io.thoth.server.database.access

import io.thoth.server.database.tables.TrackEntity

fun TrackEntity.markAsTouched() {
    scanIndex = book.library.scanIndex
}

fun TrackEntity.hasBeenUpdated(updateTime: Long) = this.accessTime >= updateTime
