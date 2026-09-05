package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.Book
import io.thoth.models.BookDetailed
import io.thoth.models.BookUpdate
import io.thoth.models.PaginatedResponse
import io.thoth.models.TitledId
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.patch
import io.thoth.openapi.ktor.post
import io.thoth.server.repositories.BookRepository
import org.koin.ktor.ext.inject
import io.thoth.server.plugins.auth.thothPrincipal

fun Routing.bookRouting() {
    val bookRepository by inject<BookRepository>()
    get<Api.Libraries.Id.Books.All, PaginatedResponse<Book>> { route ->
        val books =
            bookRepository.getAll(
                userId = thothPrincipal().userId,
                libraryId = route.libraryId,
                order = route.order.toSortOrder(),
                limit = route.limit,
                offset = route.offset,
                showInvisible = route.showInvisible,
            )
        PaginatedResponse(
            items = books,
            total = bookRepository.total(libraryId = route.libraryId, showInvisible = route.showInvisible),
            limit = route.limit,
            offset = route.offset,
        )
    }

    get<Api.Libraries.Id.Books.Id, BookDetailed> { route ->
        bookRepository.get(userId = thothPrincipal().userId, id = route.id, libraryId = route.libraryId)
    }

    get<Api.Libraries.Id.Books.Autocomplete, List<TitledId>> { route ->
        bookRepository.search(thothPrincipal().userId, route.q, route.libraryId).map { TitledId(it.id, it.title) }
    }

    patch<Api.Libraries.Id.Books.Id, BookUpdate, Book> { route, patch ->
        bookRepository.modify(thothPrincipal().userId, route.id, route.libraryId, patch)
    }

    post<Api.Libraries.Id.Books.Id.AutoMatch, Unit, Book> { id, _ ->
        bookRepository.autoMatch(thothPrincipal().userId, id.id, id.libraryId)
    }
}
