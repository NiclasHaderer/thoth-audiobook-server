package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.generators.openapi.get
import io.thoth.generators.openapi.patch
import io.thoth.generators.openapi.put
import io.thoth.models.BookModel
import io.thoth.models.DetailedBookModel
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.models.TitledId
import io.thoth.server.services.BookRepository
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.koin.ktor.ext.inject

fun Routing.bookRouting() {
    val bookRepository by inject<BookRepository>()
    get<Api.Libraries.Id.Books.All, PaginatedResponse<BookModel>> { route ->
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
        val sortOrder =
            bookRepository.position(
                libraryId = route.libraryId,
                id = route.id,
                order = SortOrder.ASC,
            )
        Position(sortIndex = sortOrder, id = route.id, order = Position.Order.ASC)
    }

    get<Api.Libraries.Id.Books.Id, DetailedBookModel> { route ->
        bookRepository.get(id = route.id, libraryId = route.libraryId)
    }

    get<Api.Libraries.Id.Books.Autocomplete, List<TitledId>> { route ->
        bookRepository.search(route.q, route.libraryId).map { TitledId(it.id, it.title) }
    }

    patch<Api.Libraries.Id.Books.Id, PartialBookApiModel, BookModel> { route, patch ->
        bookRepository.modify(route.id, route.libraryId, patch)
    }

    put<Api.Libraries.Id.Books.Id, BookApiModel, BookModel> { id, putBook ->
        bookRepository.replace(id.id, id.libraryId, putBook)
    }

    post<Api.Libraries.Id.Books.Id.AutoMatch, Unit, BookModel> { id, _ -> }
}
