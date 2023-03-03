package io.thoth.server.api.audiobooks.series

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.access.getDetailedById
import io.thoth.database.access.getMultiple
import io.thoth.database.access.positionOf
import io.thoth.database.tables.Series
import io.thoth.database.tables.TSeries
import io.thoth.models.*
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.post
import io.thoth.openapi.serverError
import io.thoth.server.api.audiobooks.QueryLimiter
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.registerSeriesRouting() =
    route("series") {
        get<QueryLimiter, PaginatedResponse<SeriesModel>> {
            transaction {
                val series = Series.getMultiple(it.limit, it.offset)
                val seriesCount = Series.count()
                PaginatedResponse(series, total = seriesCount, offset = it.offset, limit = it.limit)
            }
        }

        get<QueryLimiter, List<UUID>>("sorting") {
            transaction { Series.getMultiple(it.limit, it.offset) }.map { it.id }
        }

        get<SeriesId.Position, Position> {
            val sortOrder =
                transaction { Series.positionOf(it.parent.id) }
                    ?: serverError(HttpStatusCode.NotFound, "Could not find series")
            Position(sortIndex = sortOrder, id = it.parent.id, order = Position.Order.ASC)
        }

        get<SeriesId, DetailedSeriesModel> {
            transaction { Series.getDetailedById(it.id) }
                ?: serverError(HttpStatusCode.NotFound, "Could not find series")
        }

        get<SeriesName, List<TitledId>>("autocomplete") {
            transaction {
                Series.all().orderBy(TSeries.title to SortOrder.ASC).limit(30).map { TitledId(it.id.value, it.title) }
            }
        }

        patch(RouteHandler::patchSeries)

        post(RouteHandler::postSeries)
    }
