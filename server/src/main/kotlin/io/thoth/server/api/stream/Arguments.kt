package io.thoth.server.api.stream

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*


@Resource("{id}")
@Serializable
class AudioId(
    @Serializable(UUIDSerializer::class) val id: UUID,
)
