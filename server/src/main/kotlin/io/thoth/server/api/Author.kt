package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.Author
import io.thoth.models.AuthorCreate
import io.thoth.models.AuthorDetailed
import io.thoth.models.AuthorUpdate
import io.thoth.models.NamedId
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.patch
import io.thoth.openapi.ktor.post
import io.thoth.server.repositories.AuthorRepository
import org.koin.ktor.ext.inject
import java.util.UUID
import io.thoth.server.plugins.auth.thothPrincipal

fun Routing.authorRouting() {
    val authorService by inject<AuthorRepository>()

    get<Api.Libraries.Id.Authors.All, PaginatedResponse<Author>> {
        PaginatedResponse(
            items = authorService.getAll(thothPrincipal().userId, it.libraryId, it.order.toSortOrder(), it.limit, it.offset),
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
            sortIndex = authorService.position(id = it.id, libraryId = it.libraryId, order = it.order.toSortOrder()),
            id = it.id,
            order = it.order,
        )
    }

    get<Api.Libraries.Id.Authors.Id, AuthorDetailed> { authorService.get(thothPrincipal().userId, it.id, it.libraryId) }

    get<Api.Libraries.Id.Authors.Autocomplete, List<NamedId>> {
        authorService.search(thothPrincipal().userId, it.q, it.libraryId).map { NamedId(it.id, it.name) }
    }

    patch<Api.Libraries.Id.Authors.Id, AuthorUpdate, Author> { id, patchAuthor ->
        authorService.modify(thothPrincipal().userId, id.id, id.libraryId, patchAuthor)
    }

    post<Api.Libraries.Id.Authors, AuthorCreate, AuthorDetailed> { route, postAuthor ->
        val author = authorService.createManual(postAuthor.name, route.libraryId)
        authorService.get(thothPrincipal().userId, author.id, route.libraryId)
    }

    post<Api.Libraries.Id.Authors.Id.AutoMatch, Unit, Author> { id, _ ->
        authorService.autoMatch(thothPrincipal().userId, id.id, id.libraryId)
    }
}
