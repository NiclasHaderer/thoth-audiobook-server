package io.thoth.server.api.audiobooks

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam


class QueryLimiter(
    @QueryParam("How many items do you want to query") val limit: Int = 20,
    @QueryParam("At what position should the query start") val offset: Long = 0,
)
