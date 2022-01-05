package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class ImageModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val image: ByteArray,
)
