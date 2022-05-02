package io.thoth.models.datastructures

import java.util.*

class NamedId(
    val name: String,
    val id: UUID,
)

class TitledId(
    val title: String,
    val id: UUID,
)
