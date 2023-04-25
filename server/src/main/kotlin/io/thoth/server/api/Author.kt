package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.generators.openapi.get
import io.thoth.generators.openapi.patch
import io.thoth.generators.openapi.put
import io.thoth.models.AuthorModel
import io.thoth.models.DetailedAuthorModel
import io.thoth.models.NamedId
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.server.services.AuthorRepository
import java.util.*
import org.koin.ktor.ext.inject

fun Routing.authorRouting() {
    val authorService by inject<AuthorRepository>()

    get<Api.Libraries.Id.Authors.All, PaginatedResponse<AuthorModel>> {
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
            sortIndex = authorService.position(it.libraryId, it.authorId, it.order.toSortOrder()),
            id = it.authorId,
            order = it.order,
        )
    }

    get<Api.Libraries.Id.Authors.Id, DetailedAuthorModel> { authorService.get(it.authorId, it.libraryId) }

    get<Api.Libraries.Id.Authors.Autocomplete, List<NamedId>> {
        authorService.search(it.q, it.libraryId).map { NamedId(it.id, it.name) }
    }

    patch<Api.Libraries.Id.Authors.Id, PartialAuthorApiModel, AuthorModel> { id, patchAuthor ->
        authorService.modify(id.authorId, id.libraryId, patchAuthor)
    }

    put<Api.Libraries.Id.Authors.Id, AuthorApiModel, AuthorModel> { id, patchAuthor ->
        authorService.replace(id.authorId, id.libraryId, patchAuthor)
    }
}
