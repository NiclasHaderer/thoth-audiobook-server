package io.thoth.server.api

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.common.extensions.toSizedIterable
import io.thoth.database.access.getDetailedById
import io.thoth.database.access.getMultiple
import io.thoth.database.access.getNewImage
import io.thoth.database.access.positionOf
import io.thoth.database.access.toModel
import io.thoth.database.tables.Author
import io.thoth.database.tables.Book
import io.thoth.database.tables.Image
import io.thoth.database.tables.Series
import io.thoth.database.tables.TSeries
import io.thoth.models.DetailedSeriesModel
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.models.SeriesModel
import io.thoth.models.TitledId
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.put
import io.thoth.openapi.serverError
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

fun Routing.seriesRouting() {
    // TODO limit to library
    get<Api.Libraries.Id.Series.All, PaginatedResponse<SeriesModel>> { (limit, offset) ->
        transaction {
            val series = Series.getMultiple(limit, offset)
            val seriesCount = Series.count()
            PaginatedResponse(series, total = seriesCount, offset = offset, limit = limit)
        }
    }

    get<Api.Libraries.Id.Series.Sorting, List<UUID>> { (limit, offset) ->
        transaction { Series.getMultiple(limit, offset) }.map { it.id }
    }

    get<Api.Libraries.Id.Series.Id.Position, Position> { route ->
        val sortOrder =
            transaction { Series.positionOf(route.id) } ?: serverError(HttpStatusCode.NotFound, "Could not find series")
        Position(sortIndex = sortOrder, id = route.id, order = Position.Order.ASC)
    }

    get<Api.Libraries.Id.Series.Id, DetailedSeriesModel> { (id) ->
        transaction { Series.getDetailedById(id) } ?: serverError(HttpStatusCode.NotFound, "Could not find series")
    }

    get<Api.Libraries.Id.Series.Autocomplete, List<TitledId>> { (q) ->
        transaction {
            Series.find { TSeries.title like "%$q%" }
                .orderBy(TSeries.title to SortOrder.ASC)
                .limit(30)
                .map { TitledId(it.id.value, it.title) }
        }
    }

    patch<Api.Libraries.Id.Series.Id, PatchSeries, SeriesModel> { seriesId, patchSeries ->
        transaction {
            val series =
                Series.findById(seriesId.seriesId) ?: serverError(HttpStatusCode.NotFound, "Could not find series")

            series.apply {
                title = patchSeries.title ?: title
                provider = patchSeries.provider ?: provider
                providerID = patchSeries.providerID ?: providerID
                totalBooks = patchSeries.totalBooks ?: totalBooks
                primaryWorks = patchSeries.primaryWorks ?: primaryWorks
                coverID = Image.getNewImage(patchSeries.cover, currentImageID = coverID, default = coverID)
                description = patchSeries.description ?: description
            }

            if (patchSeries.authors != null) {
                series.authors =
                    patchSeries.authors
                        .map { Author.findById(it) ?: serverError(HttpStatusCode.NotFound, "Could not find author") }
                        .toSizedIterable()
            }

            if (patchSeries.books != null) {
                series.books =
                    patchSeries.books
                        .map { Book.findById(it) ?: serverError(HttpStatusCode.NotFound, "Could not find book") }
                        .toSizedIterable()
            }

            series.toModel()
        }
    }

    put<Api.Libraries.Id.Series.Id, PutSeries, SeriesModel> { seriesId, putSeries ->
        transaction {
            val series =
                Series.findById(seriesId.seriesId) ?: serverError(HttpStatusCode.NotFound, "Could not find series")

            series.apply {
                title = putSeries.title
                provider = putSeries.provider
                providerID = putSeries.providerID
                totalBooks = putSeries.totalBooks
                primaryWorks = putSeries.primaryWorks
                coverID = Image.getNewImage(putSeries.cover, currentImageID = coverID, default = null)
                description = putSeries.description
                series.authors =
                    putSeries.authors
                        .map { Author.findById(it) ?: serverError(HttpStatusCode.NotFound, "Could not find author") }
                        .toSizedIterable()
                series.books =
                    putSeries.books
                        .map { Book.findById(it) ?: serverError(HttpStatusCode.NotFound, "Could not find book") }
                        .toSizedIterable()
            }
            series.toModel()
        }
    }
}
