package io.thoth.models

import java.util.UUID

data class Chapter(
    val title: String?,
    val startMs: Long,
    val endMs: Long,
    val trackId: UUID,
)
