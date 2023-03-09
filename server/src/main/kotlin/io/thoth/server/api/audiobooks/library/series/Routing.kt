package io.thoth.server.api.audiobooks.library.series

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.access.getDetailedById
import io.thoth.database.access.getMultiple
import io.thoth.database.access.positionOf
import io.thoth.database.tables.Series
import io.thoth.database.tables.TSeries
import io.thoth.models.DetailedSeriesModel
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.models.SeriesModel
import io.thoth.models.TitledId
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.post
import io.thoth.openapi.serverError
import io.thoth.server.api.audiobooks.QueryLimiter
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.registerSeriesRouting() {
    route("series") {
        get<QueryLimiter, PaginatedResponse<SeriesModel>> { (limit, offset) ->
            transaction {
                val series = Series.getMultiple(limit, offset)
                val seriesCount = Series.count()
                PaginatedResponse(series, total = seriesCount, offset = offset, limit = limit)
            }
        }

        get<QueryLimiter, List<UUID>>("sorting") { (limit, offset) ->
            transaction { Series.getMultiple(limit, offset) }.map { it.id }
        }

        get<SeriesId.Position, Position> { (route) ->
            val sortOrder =
                transaction { Series.positionOf(route.id) }
                    ?: serverError(HttpStatusCode.NotFound, "Could not find series")
            Position(sortIndex = sortOrder, id = route.id, order = Position.Order.ASC)
        }

        get<SeriesId, DetailedSeriesModel> { (id) ->
            transaction { Series.getDetailedById(id) } ?: serverError(HttpStatusCode.NotFound, "Could not find series")
        }

        get<SeriesName, List<TitledId>>("autocomplete") {
            transaction {
                Series.all().orderBy(TSeries.title to SortOrder.ASC).limit(30).map { TitledId(it.id.value, it.title) }
            }
        }

        patch(RouteHandler::patchSeries)

        post(RouteHandler::postSeries)
    }
}
