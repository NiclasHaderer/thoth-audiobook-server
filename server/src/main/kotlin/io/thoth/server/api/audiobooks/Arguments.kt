package io.thoth.server.api.audiobooks

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("")
class QueryLimiter(
    val limit: Int = 20,
    val offset: Long = 0,
)
