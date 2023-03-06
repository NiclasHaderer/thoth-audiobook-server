package io.thoth.server.api.audiobooks

import io.ktor.resources.*

@Resource("")
data class QueryLimiter(
    val limit: Int = 20,
    val offset: Long = 0,
)
