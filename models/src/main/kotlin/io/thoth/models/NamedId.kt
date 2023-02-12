package io.thoth.models

import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable

@Serializable
data class NamedId(
    val name: String,
    val id: UUID_S,
)

@Serializable
data class TitledId(
    val title: String,
    val id: UUID_S,
)
