package io.thoth.models

import java.util.UUID

data class Track(
    val id: UUID,
    val title: String,
    val trackNr: Int?,
    val durationMs: Long,
    val fileModifiedAt: Long,
    val book: TitledId,
)
