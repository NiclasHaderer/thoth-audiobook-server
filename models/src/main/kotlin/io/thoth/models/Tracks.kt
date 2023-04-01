package io.thoth.models

import java.time.LocalDateTime
import java.util.*

data class TrackModel(
    val id: UUID,
    val title: String,
    val trackNr: Int?,
    val duration: Int,
    val accessTime: Long,
    val book: TitledId,
    val path: String,
    val updateTime: LocalDateTime,
)
