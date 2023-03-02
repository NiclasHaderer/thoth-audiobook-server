package io.thoth.server.api.audiobooks.books

import io.ktor.http.*
import io.thoth.common.extensions.toSizedIterable
import io.thoth.database.access.getNewImage
import io.thoth.database.access.toModel
import io.thoth.database.tables.Author
import io.thoth.database.tables.Book
import io.thoth.database.tables.Image
import io.thoth.database.tables.Series
import io.thoth.models.BookModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction

internal fun RouteHandler.patchBook(id: BookId, patchBook: PatchBook): BookModel = transaction {
  val book = Book.findById(id.id) ?: serverError(HttpStatusCode.NotFound, "Book was not found")
  book.apply {
    title = patchBook.title ?: title
    provider = patchBook.provider ?: provider
    providerID = patchBook.providerID ?: providerID
    providerRating = patchBook.providerRating ?: providerRating
    releaseDate = patchBook.releaseDate ?: releaseDate
    publisher = patchBook.publisher ?: publisher
    language = patchBook.language ?: language
    description = patchBook.description ?: description
    narrator = patchBook.narrator ?: narrator
    isbn = patchBook.isbn ?: isbn
    coverID = Image.getNewImage(patchBook.cover, currentImageID = coverID, default = coverID)
  }
  if (patchBook.authors != null) {
    book.authors =
        patchBook.authors
            .map {
              Author.findById(it) ?: serverError(HttpStatusCode.NotFound, "Author was not found")
            }
            .toSizedIterable()
  }
  if (patchBook.series != null) {
    book.series =
        patchBook.series
            .map {
              Series.findById(it) ?: serverError(HttpStatusCode.NotFound, "Series was not found")
            }
            .toSizedIterable()
  }
  book.toModel()
}

internal fun RouteHandler.postBook(id: BookId, postBook: PostBook): BookModel = transaction {
  val book = Book.findById(id.id) ?: serverError(HttpStatusCode.NotFound, "Book was not found")
  book.apply {
    title = postBook.title
    provider = postBook.provider
    providerID = postBook.providerID
    providerRating = postBook.providerRating
    releaseDate = postBook.releaseDate
    publisher = postBook.publisher
    language = postBook.language
    description = postBook.description
    narrator = postBook.narrator
    isbn = postBook.isbn
    coverID = Image.getNewImage(postBook.cover, currentImageID = coverID, default = null)
    authors =
        postBook.authors
            .map {
              Author.findById(it) ?: serverError(HttpStatusCode.NotFound, "Author was not found")
            }
            .toSizedIterable()
    series =
        postBook.series
            ?.map {
              Series.findById(it) ?: serverError(HttpStatusCode.NotFound, "Series was not found")
            }
            ?.toSizedIterable()
            ?: emptyList<Series>().toSizedIterable()
  }
  book.toModel()
}
