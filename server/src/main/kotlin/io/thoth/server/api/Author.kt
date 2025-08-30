package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.Author
import io.thoth.models.DetailedAuthor
import io.thoth.models.NamedId
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.patch
import io.thoth.openapi.ktor.put
import io.thoth.server.repositories.AuthorRepository
import org.koin.ktor.ext.inject
import java.util.UUID

fun Routing.authorRouting() {
    val authorService by inject<AuthorRepository>()

    get<Api.Libraries.Id.Authors.All, PaginatedResponse<Author>> {
        PaginatedResponse(
            items = authorService.getAll(it.libraryId, it.order.toSortOrder(), it.limit, it.offset),
            limit = it.limit,
            offset = it.offset,
            total = authorService.total(it.libraryId),
        )
    }
    get<Api.Libraries.Id.Authors.Sorting, List<UUID>> {
        authorService.sorting(it.libraryId, it.order.toSortOrder(), it.limit, it.offset)
    }

    get<Api.Libraries.Id.Authors.Id.Position, Position> {
        Position(
            sortIndex = authorService.position(it.libraryId, it.id, it.order.toSortOrder()),
            id = it.id,
            order = it.order,
        )
    }

    get<Api.Libraries.Id.Authors.Id, DetailedAuthor> { authorService.get(it.id, it.libraryId) }

    get<Api.Libraries.Id.Authors.Autocomplete, List<NamedId>> {
        authorService.search(it.q, it.libraryId).map { NamedId(it.id, it.name) }
    }

    patch<Api.Libraries.Id.Authors.Id, PartialAuthorApiModel, Author> { id, patchAuthor ->
        authorService.modify(id.id, id.libraryId, patchAuthor)
    }

    put<Api.Libraries.Id.Authors.Id, AuthorApiModel, Author> { id, patchAuthor ->
        authorService.replace(id.id, id.libraryId, patchAuthor)
    }
}
