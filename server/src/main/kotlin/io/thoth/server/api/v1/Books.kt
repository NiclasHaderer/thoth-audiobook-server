package io.thoth.server.api.v1

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.common.extensions.toSizedIterable
import io.thoth.database.access.getDetailedById
import io.thoth.database.access.getMultiple
import io.thoth.database.access.getNewImage
import io.thoth.database.access.positionOf
import io.thoth.database.access.toModel
import io.thoth.database.tables.Author
import io.thoth.database.tables.Book
import io.thoth.database.tables.Image
import io.thoth.database.tables.Series
import io.thoth.database.tables.TBooks
import io.thoth.models.BookModel
import io.thoth.models.DetailedBookModel
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.models.TitledId
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.put
import io.thoth.openapi.serverError
import io.thoth.server.api.Api
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction

fun Routing.bookRouting() {
    get<Api.Libraries.Books.All, PaginatedResponse<BookModel>> { (limit, offset) ->
        transaction {
            val books = Book.getMultiple(limit, offset)
            val total = Book.count()
            PaginatedResponse(books, total = total, offset = offset, limit = limit)
        }
    }

    get<Api.Libraries.Books.Sorting, List<UUID>> { (limit, offset) ->
        transaction { Book.getMultiple(limit, offset).map { it.id } }
    }

    get<Api.Libraries.Books.Id.Position, Position> { route ->
        val sortOrder =
            transaction { Book.positionOf(route.id) } ?: serverError(HttpStatusCode.NotFound, "Could not find book")
        Position(sortIndex = sortOrder, id = route.id, order = Position.Order.ASC)
    }

    get<Api.Libraries.Books.Id, DetailedBookModel> {
        transaction { Book.getDetailedById(it.id) } ?: serverError(HttpStatusCode.NotFound, "Could not find book")
    }

    get<Api.Libraries.Books.Autocomplete, List<TitledId>> { route ->
        transaction {
            Book.find { TBooks.title like "%${route.q}%" }
                .orderBy(TBooks.title.lowerCase() to SortOrder.ASC)
                .limit(10)
                .map { TitledId(it.id.value, it.title) }
        }
    }

    patch<Api.Libraries.Books.Id, PartialBookApiModel, BookModel> { route, patch ->
        transaction {
            val book = Book.findById(route.id) ?: serverError(HttpStatusCode.NotFound, "Book was not found")
            book.apply {
                title = patch.title ?: title
                provider = patch.provider ?: provider
                providerID = patch.providerID ?: providerID
                providerRating = patch.providerRating ?: providerRating
                releaseDate = patch.releaseDate ?: releaseDate
                publisher = patch.publisher ?: publisher
                language = patch.language ?: language
                description = patch.description ?: description
                narrator = patch.narrator ?: narrator
                isbn = patch.isbn ?: isbn
                coverID = Image.getNewImage(patch.cover, currentImageID = coverID, default = coverID)
            }
            if (patch.authors != null) {
                book.authors =
                    patch.authors
                        .map { Author.findById(it) ?: serverError(HttpStatusCode.NotFound, "Author was not found") }
                        .toSizedIterable()
            }
            if (patch.series != null) {
                book.series =
                    patch.series
                        .map { Series.findById(it) ?: serverError(HttpStatusCode.NotFound, "Series was not found") }
                        .toSizedIterable()
            }
            book.toModel()
        }
    }

    put<Api.Libraries.Books.Id, BookApiModel, BookModel> { id, putBook ->
        transaction {
            val book = Book.findById(id.id) ?: serverError(HttpStatusCode.NotFound, "Book was not found")
            book.apply {
                title = putBook.title
                provider = putBook.provider
                providerID = putBook.providerID
                providerRating = putBook.providerRating
                releaseDate = putBook.releaseDate
                publisher = putBook.publisher
                language = putBook.language
                description = putBook.description
                narrator = putBook.narrator
                isbn = putBook.isbn
                coverID = Image.getNewImage(putBook.cover, currentImageID = coverID, default = null)
                authors =
                    putBook.authors
                        .map { Author.findById(it) ?: serverError(HttpStatusCode.NotFound, "Author was not found") }
                        .toSizedIterable()
                series =
                    putBook.series
                        ?.map { Series.findById(it) ?: serverError(HttpStatusCode.NotFound, "Series was not found") }
                        ?.toSizedIterable()
                        ?: emptyList<Series>().toSizedIterable()
            }
            book.toModel()
        }
    }
}
