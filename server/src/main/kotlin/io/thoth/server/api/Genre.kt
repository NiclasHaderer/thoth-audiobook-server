package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.Genre
import io.thoth.models.GenreDetailed
import io.thoth.models.PaginatedResponse
import io.thoth.openapi.ktor.get
import io.thoth.server.plugins.auth.thothPrincipal
import io.thoth.server.repositories.GenreRepository
import org.koin.ktor.ext.inject

fun Routing.genreRouting() {
    val genreRepository by inject<GenreRepository>()

    get<Api.Libraries.Id.Genres.All, PaginatedResponse<Genre>> {
        PaginatedResponse(
            items = genreRepository.getAll(it.libraryId, it.order.toSortOrder(), it.limit, it.offset),
            limit = it.limit,
            offset = it.offset,
            total = genreRepository.total(it.libraryId),
        )
    }

    get<Api.Libraries.Id.Genres.Name, GenreDetailed> {
        genreRepository.get(
            thothPrincipal().userId,
            it.name,
            it.libraryId,
        )
    }
}
