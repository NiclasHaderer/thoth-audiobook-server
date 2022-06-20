package io.thoth.server.api.audiobooks.series

import io.ktor.http.*
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError

internal suspend fun RouteHandler.patchSeries(
    seriesId: SeriesId,
    patchSeries: PatchSeries,
) {
    serverError(HttpStatusCode.NotImplemented, "Series modification is not yet supported")
}
