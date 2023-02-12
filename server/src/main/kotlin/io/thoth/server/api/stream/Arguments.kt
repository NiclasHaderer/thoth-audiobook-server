package io.thoth.server.api.stream

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable
import java.util.*


@Resource("{id}")
@Serializable
class AudioId(
    val id: UUID_S,
)
