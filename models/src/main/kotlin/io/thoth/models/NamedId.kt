package io.thoth.models

import io.thoth.common.serializion.kotlin.UUID_S

data class NamedId(val id: UUID_S, val name: String)

data class TitledId(val id: UUID_S, val title: String)
