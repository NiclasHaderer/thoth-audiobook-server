package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.DetailedSeries
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.models.Series
import io.thoth.models.TitledId
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.patch
import io.thoth.openapi.ktor.put
import io.thoth.server.repositories.SeriesRepository
import org.jetbrains.exposed.v1.core.SortOrder
import org.koin.ktor.ext.inject
import java.util.UUID

fun Routing.seriesRouting() {
    val seriesRepository by inject<SeriesRepository>()
    get<Api.Libraries.Id.Series.All, PaginatedResponse<Series>> {
        PaginatedResponse(
            seriesRepository.getAll(
                libraryId = it.libraryId,
                order = SortOrder.ASC,
                limit = it.limit,
                offset = it.offset,
            ),
            offset = it.offset,
            limit = it.limit,
            total = seriesRepository.total(libraryId = it.libraryId),
        )
    }

    get<Api.Libraries.Id.Series.Sorting, List<UUID>> {
        seriesRepository.sorting(
            libraryId = it.libraryId,
            order = it.order.toSortOrder(),
            limit = it.limit,
            offset = it.offset,
        )
    }

    get<Api.Libraries.Id.Series.Id.Position, Position> {
        Position(
            sortIndex = seriesRepository.position(it.id, it.libraryId, it.order.toSortOrder()),
            id = it.id,
            order = it.order,
        )
    }

    get<Api.Libraries.Id.Series.Id, DetailedSeries> { seriesRepository.get(id = it.id, libraryId = it.libraryId) }

    get<Api.Libraries.Id.Series.Autocomplete, List<TitledId>> {
        seriesRepository
            .search(query = it.q, libraryId = it.libraryId)
            .map { series -> TitledId(id = series.id, title = series.title) }
    }

    patch<Api.Libraries.Id.Series.Id, PartialSeriesApiModel, Series> { id, patchSeries ->
        seriesRepository.modify(id = id.id, libraryId = id.libraryId, partial = patchSeries)
    }

    put<Api.Libraries.Id.Series.Id, SeriesApiModel, Series> { id, putSeries ->
        seriesRepository.replace(id = id.id, libraryId = id.libraryId, complete = putSeries)
    }
}
