package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.models.Series
import io.thoth.models.SeriesCreate
import io.thoth.models.SeriesDetailed
import io.thoth.models.SeriesUpdate
import io.thoth.models.TitledId
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.patch
import io.thoth.openapi.ktor.post
import io.thoth.server.repositories.SeriesRepository
import org.koin.ktor.ext.inject
import java.util.UUID
import io.thoth.server.plugins.auth.thothPrincipal

fun Routing.seriesRouting() {
    val seriesRepository by inject<SeriesRepository>()
    get<Api.Libraries.Id.Series.All, PaginatedResponse<Series>> {
        PaginatedResponse(
            seriesRepository.getAll(
                userId = thothPrincipal().userId,
                libraryId = it.libraryId,
                order = it.order.toSortOrder(),
                limit = it.limit,
                offset = it.offset,
                showInvisible = it.showInvisible,
            ),
            offset = it.offset,
            limit = it.limit,
            total = seriesRepository.total(libraryId = it.libraryId, showInvisible = it.showInvisible),
        )
    }

    get<Api.Libraries.Id.Series.Sorting, List<UUID>> {
        seriesRepository.sorting(
            libraryId = it.libraryId,
            order = it.order.toSortOrder(),
            limit = it.limit,
            offset = it.offset,
            showInvisible = it.showInvisible,
        )
    }

    get<Api.Libraries.Id.Series.Id.Position, Position> {
        Position(
            sortIndex = seriesRepository.position(it.id, it.libraryId, it.order.toSortOrder(), it.showInvisible),
            id = it.id,
            order = it.order,
        )
    }

    get<Api.Libraries.Id.Series.Id, SeriesDetailed> { seriesRepository.get(userId = thothPrincipal().userId, id = it.id, libraryId = it.libraryId) }

    get<Api.Libraries.Id.Series.Autocomplete, List<TitledId>> {
        seriesRepository
            .search(userId = thothPrincipal().userId, query = it.q, libraryId = it.libraryId)
            .map { series -> TitledId(id = series.id, title = series.title) }
    }

    patch<Api.Libraries.Id.Series.Id, SeriesUpdate, Series> { id, patchSeries ->
        seriesRepository.modify(userId = thothPrincipal().userId, id = id.id, libraryId = id.libraryId, partial = patchSeries)
    }

    post<Api.Libraries.Id.Series, SeriesCreate, SeriesDetailed> { route, postSeries ->
        val series = seriesRepository.createManual(postSeries.title, route.libraryId)
        seriesRepository.get(thothPrincipal().userId, series.id, route.libraryId)
    }

    post<Api.Libraries.Id.Series.Id.AutoMatch, Unit, Series> { id, _ ->
        seriesRepository.autoMatch(thothPrincipal().userId, id.id, id.libraryId)
    }
}
