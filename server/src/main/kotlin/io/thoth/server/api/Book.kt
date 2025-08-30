package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.Book
import io.thoth.models.DetailedBook
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.models.TitledId
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.patch
import io.thoth.openapi.ktor.post
import io.thoth.openapi.ktor.put
import io.thoth.server.repositories.BookRepository
import org.jetbrains.exposed.v1.core.SortOrder
import org.koin.ktor.ext.inject
import java.util.UUID

fun Routing.bookRouting() {
    val bookRepository by inject<BookRepository>()
    get<Api.Libraries.Id.Books.All, PaginatedResponse<Book>> { route ->
        val books =
            bookRepository.getAll(
                libraryId = route.libraryId,
                order = SortOrder.ASC,
                limit = route.limit,
                offset = route.offset,
            )
        PaginatedResponse(
            items = books,
            total = bookRepository.total(libraryId = route.libraryId),
            limit = route.limit,
            offset = route.offset,
        )
    }

    get<Api.Libraries.Id.Books.Sorting, List<UUID>> { route ->
        bookRepository.sorting(
            libraryId = route.libraryId,
            order = SortOrder.ASC,
            limit = route.limit,
            offset = route.offset,
        )
    }

    get<Api.Libraries.Id.Books.Id.Position, Position> { route ->
        val sortOrder = bookRepository.position(libraryId = route.libraryId, id = route.id, order = SortOrder.ASC)
        Position(sortIndex = sortOrder, id = route.id, order = Position.Order.ASC)
    }

    get<Api.Libraries.Id.Books.Id, DetailedBook> { route ->
        bookRepository.get(id = route.id, libraryId = route.libraryId)
    }

    get<Api.Libraries.Id.Books.Autocomplete, List<TitledId>> { route ->
        bookRepository.search(route.q, route.libraryId).map { TitledId(it.id, it.title) }
    }

    patch<Api.Libraries.Id.Books.Id, PartialBookApiModel, Book> { route, patch ->
        bookRepository.modify(route.id, route.libraryId, patch)
    }

    put<Api.Libraries.Id.Books.Id, BookApiModel, Book> { id, putBook ->
        bookRepository.replace(id.id, id.libraryId, putBook)
    }

    post<Api.Libraries.Id.Books.Id.AutoMatch, Unit, Book> { id, _ ->
        bookRepository.autoMatch(id.id, id.libraryId)
    }
}
