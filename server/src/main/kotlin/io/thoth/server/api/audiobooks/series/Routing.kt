package io.thoth.server.api.audiobooks.series

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.tables.Series
import io.thoth.models.PaginatedResponse
import io.thoth.models.SeriesModel
import io.thoth.models.SeriesModelWithBooks
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.serverError
import io.thoth.server.api.audiobooks.QueryLimiter
import java.util.*


fun Route.registerSeriesRouting(path: String = "series") {
    route(path) {
        routing()
    }
}

internal fun Route.routing() {
    get<QueryLimiter, PaginatedResponse<SeriesModel>> {
        val series = Series.getMultiple(it.limit, it.offset)
        val seriesCount = Series.totalCount()
        PaginatedResponse(series, total = seriesCount, offset = it.offset, limit = it.limit)
    }
    get<QueryLimiter, List<UUID>>("sorting") { query ->
        Series.getMultiple(query.limit, query.offset).map { it.id }
    }
    get<SeriesId, SeriesModelWithBooks> {
        Series.getById(it.id) ?: serverError(HttpStatusCode.NotFound, "Could not find series")
    }

    patch(RouteHandler::patchSeries)
}
