package io.thoth.models

import java.time.LocalDateTime
import java.util.UUID

data class Track(
    val id: UUID,
    val title: String,
    val trackNr: Int?,
    val duration: Int,
    val accessTime: Long,
    val book: TitledId,
    val path: String,
    val updateTime: LocalDateTime,
    val library: NamedId,
)
