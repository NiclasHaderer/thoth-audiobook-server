package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class NamedId(
    val name: String,
    @Serializable(UUIDSerializer::class) val id: UUID,
)

@Serializable
class TitledId(
    val title: String,
    @Serializable(UUIDSerializer::class) val id: UUID,
)
