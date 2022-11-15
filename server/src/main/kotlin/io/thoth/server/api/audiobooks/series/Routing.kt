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
import io.thoth.server.db.access.getDetailedById
import io.thoth.server.db.access.getMultiple
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


fun Route.registerSeriesRouting(path: String = "series") {
    route(path) {
        routing()
    }
}

internal fun Route.routing() {
    get<QueryLimiter, PaginatedResponse<SeriesModel>> {
        transaction {
            val seriesCount: Long
            val series: List<SeriesModel>
            series = Series.getMultiple(it.limit, it.offset)
            seriesCount = Series.count()
            PaginatedResponse(series, total = seriesCount, offset = it.offset, limit = it.limit)
        }
    }
    get<QueryLimiter, List<UUID>>("sorting") {
        transaction { Series.getMultiple(it.limit, it.offset) }.map { it.id }
    }
    get<SeriesId, SeriesModelWithBooks> {
        transaction { Series.getDetailedById(it.id) } ?: serverError(HttpStatusCode.NotFound, "Could not find series")
    }

    patch(RouteHandler::patchSeries)
}
