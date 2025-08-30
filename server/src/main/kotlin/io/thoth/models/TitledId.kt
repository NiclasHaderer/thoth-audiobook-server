package io.thoth.models

import java.util.UUID

data class TitledId(
    val id: UUID,
    val title: String,
)
