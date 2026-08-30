package io.thoth.models

import java.time.Instant
import java.util.UUID

data class ListeningHistoryEntry(
    val id: UUID,
    val book: Book,
    val positionMs: Long,
    val at: Instant,
)
