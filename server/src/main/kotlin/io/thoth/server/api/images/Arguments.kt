package io.thoth.server.api.images

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable
import java.util.*


@Serializable
@Resource("{id}")
internal class ImageId(
    val id: UUID_S,
)
