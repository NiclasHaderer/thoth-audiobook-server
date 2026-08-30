package io.thoth.models

import java.time.Instant
import java.util.UUID

data class Track(
    val id: UUID,
    val title: String,
    val trackNr: Int?,
    val durationMs: Long,
    val fileModifiedAt: Instant,
    val book: TitledId,
)
