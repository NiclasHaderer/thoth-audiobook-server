package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.models.DetailedSeriesModel
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.models.SeriesModel
import io.thoth.models.TitledId
import io.thoth.openapi.openapi.get
import io.thoth.openapi.openapi.patch
import io.thoth.openapi.openapi.put
import io.thoth.server.repositories.SeriesRepository
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.koin.ktor.ext.inject

fun Routing.seriesRouting() {
    val seriesRepository by inject<SeriesRepository>()
    get<Api.Libraries.Id.Series.All, PaginatedResponse<SeriesModel>> {
        PaginatedResponse(
            seriesRepository.getAll(it.libraryId, SortOrder.ASC, it.limit, it.offset),
            offset = it.offset,
            limit = it.limit,
            total = seriesRepository.total(it.libraryId),
        )
    }

    get<Api.Libraries.Id.Series.Sorting, List<UUID>> {
        seriesRepository.sorting(it.libraryId, order = it.order.toSortOrder(), limit = it.limit, offset = it.offset)
    }

    get<Api.Libraries.Id.Series.Id.Position, Position> {
        Position(
            sortIndex = seriesRepository.position(it.id, it.libraryId, it.order.toSortOrder()),
            id = it.id,
            order = it.order,
        )
    }

    get<Api.Libraries.Id.Series.Id, DetailedSeriesModel> { seriesRepository.get(it.libraryId, it.id) }

    get<Api.Libraries.Id.Series.Autocomplete, List<TitledId>> {
        seriesRepository.search(it.q, it.libraryId).map { series -> TitledId(series.id, series.title) }
    }

    patch<Api.Libraries.Id.Series.Id, PartialSeriesApiModel, SeriesModel> { id, patchSeries ->
        seriesRepository.modify(id.id, id.libraryId, patchSeries)
    }

    put<Api.Libraries.Id.Series.Id, SeriesApiModel, SeriesModel> { id, putSeries ->
        seriesRepository.replace(id.id, id.libraryId, putSeries)
    }
}
