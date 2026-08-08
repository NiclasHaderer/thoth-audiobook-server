package io.thoth.models

import java.util.UUID

data class Track(
    val id: UUID,
    val title: String,
    val trackNr: Int?,
    val duration: Int,
    val fileModifiedAt: Long,
    val book: TitledId,
)
