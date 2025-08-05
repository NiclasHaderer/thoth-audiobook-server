package io.thoth.models

import java.util.*

data class NamedId(val id: UUID, val name: String)

data class TitledId(val id: UUID, val title: String)
