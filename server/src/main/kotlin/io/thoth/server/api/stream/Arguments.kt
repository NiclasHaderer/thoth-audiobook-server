package io.thoth.server.api.stream

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import java.util.*
import kotlinx.serialization.Serializable

@Resource("{id}")
@Serializable
class AudioId(
    val id: UUID_S,
)
