package io.github.huiibuh.models

import java.time.LocalDateTime
import java.util.*

class TrackModel(
    val id: UUID,
    val title: String,
    val trackNr: Int?,
    val duration: Int,
    val accessTime: Long,
    val book: TitledId,
    val path: String,
    val updateTime: LocalDateTime,
)
