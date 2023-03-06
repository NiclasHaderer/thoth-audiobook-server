package io.thoth.server.api.images

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import java.util.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("{id}")
data class ImageId(
    val id: UUID_S,
)
