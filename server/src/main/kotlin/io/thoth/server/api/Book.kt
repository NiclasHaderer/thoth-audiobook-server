package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.models.BookModel
import io.thoth.models.DetailedBookModel
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.models.TitledId
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.put
import io.thoth.server.services.BookService
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.koin.ktor.ext.inject

fun Routing.bookRouting() {
    val bookService by inject<BookService>()
    get<Api.Libraries.Id.Books.All, PaginatedResponse<BookModel>> { route ->
        val books =
            bookService.books(
                libraryId = route.libraryId,
                order = SortOrder.ASC,
                limit = route.limit,
                offset = route.offset,
            )
        PaginatedResponse(
            items = books,
            total = bookService.total,
            limit = route.limit,
            offset = route.offset,
        )
    }

    get<Api.Libraries.Id.Books.Sorting, List<UUID>> { route ->
        bookService.booksSorting(
            libraryId = route.libraryId,
            order = SortOrder.ASC,
            limit = route.limit,
            offset = route.offset,
        )
    }

    get<Api.Libraries.Id.Books.Id.Position, Position> { route ->
        val sortOrder =
            bookService.bookPosition(
                libraryId = route.libraryId,
                id = route.id,
                order = SortOrder.ASC,
            )
        Position(sortIndex = sortOrder, id = route.id, order = Position.Order.ASC)
    }

    get<Api.Libraries.Id.Books.Id, DetailedBookModel> { route ->
        bookService.book(id = route.id, libraryId = route.libraryId)
    }

    get<Api.Libraries.Id.Books.Autocomplete, List<TitledId>> { route ->
        bookService.search(route.q, route.libraryId).map { TitledId(it.id, it.title) }
    }

    patch<Api.Libraries.Id.Books.Id, PartialBookApiModel, BookModel> { route, patch ->
        bookService.patchBook(route.id, route.libraryId, patch)
    }

    put<Api.Libraries.Id.Books.Id, BookApiModel, BookModel> { id, putBook ->
        bookService.replaceBook(id.id, id.libraryId, putBook)
    }
}
