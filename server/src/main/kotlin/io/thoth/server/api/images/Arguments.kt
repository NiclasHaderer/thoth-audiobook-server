package io.thoth.server.api.images

import io.ktor.resources.*
import io.thoth.common.serializion.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*


@Serializable
@Resource("{id}")
internal class ImageId(
    @Serializable(UUIDSerializer::class) val id: UUID,
)
