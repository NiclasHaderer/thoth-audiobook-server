package io.thoth.models

import io.thoth.common.serializion.kotlin.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class NamedId(
    val name: String,
    @Serializable(UUIDSerializer::class) val id: UUID,
)

@Serializable
data class TitledId(
    val title: String,
    @Serializable(UUIDSerializer::class) val id: UUID,
)
