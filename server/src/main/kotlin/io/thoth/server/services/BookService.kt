package io.thoth.server.services

import io.ktor.http.*
import io.thoth.common.extensions.toSizedIterable
import io.thoth.common.utils.take
import io.thoth.database.access.getNewImage
import io.thoth.database.access.toModel
import io.thoth.database.tables.*
import io.thoth.models.BookModel
import io.thoth.models.DetailedBookModel
import io.thoth.models.NamedId
import io.thoth.models.TitledId
import io.thoth.openapi.ErrorResponse
import io.thoth.server.api.BookApiModel
import io.thoth.server.api.PartialBookApiModel
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface BookService {
    fun books(libraryId: UUID, order: SortOrder, limit: Int = 20, offset: Long = 0L): List<BookModel>
    fun book(id: UUID, libraryId: UUID): DetailedBookModel
    fun bookPosition(libraryId: UUID, id: UUID, order: SortOrder): Long
    fun booksSorting(libraryId: UUID, order: SortOrder, limit: Int = 20, offset: Long = 0L): List<UUID>
    fun search(query: String, libraryId: UUID): List<BookModel>
    fun search(query: String): List<BookModel>
    fun patchBook(id: UUID, libraryId: UUID, partialBook: PartialBookApiModel): BookModel
    fun replaceBook(id: UUID, libraryId: UUID, partialBook: BookApiModel): BookModel

    fun toModel(book: Book, order: SortOrder = SortOrder.ASC): BookModel

    val total: Long
}

class BookServiceImpl : BookService {
    private val searchLimit = 30

    override val total: Long
        get() = transaction { Book.count() }

    override fun toModel(book: Book, order: SortOrder): BookModel = transaction {
        val preferEmbedded = book.library.preferEmbeddedMetadata
        BookModel(
            id = book.id.value,
            authors =
                book.authors
                    .sortedBy { it.name.lowercase() }
                    .map { NamedId(it.id.value, it.name) }
                    .let { if (order == SortOrder.DESC) it.reversed() else it },
            series =
                book.series
                    .sortedBy { it.title.lowercase() }
                    .map { TitledId(it.id.value, it.title) }
                    .let { if (order == SortOrder.DESC) it.reversed() else it },
            title = book.title.take(preferEmbedded, book.meta.title),
            providerID = book.meta.providerID,
            providerRating = book.meta.providerRating,
            provider = book.meta.provider,
            releaseDate = book.releaseDate.take(preferEmbedded, book.meta.releaseDate),
            publisher = book.publisher.take(preferEmbedded, book.meta.publisher),
            language = book.language.take(preferEmbedded, book.meta.language),
            description = book.description.take(preferEmbedded, book.meta.description),
            narrator = book.narrator.take(preferEmbedded, book.meta.narrator),
            isbn = book.narrator.take(preferEmbedded, book.meta.isbn),
            coverID = book.coverID?.value.take(preferEmbedded, book.meta.coverID?.value),
        )
    }

    override fun books(libraryId: UUID, order: SortOrder, limit: Int, offset: Long): List<BookModel> = transaction {
        Book.find { TBooks.library eq libraryId }
            .orderBy(TBooks.title.lowerCase() to order)
            .limit(limit, offset)
            .map { it.toModel() }
    }

    override fun book(id: UUID, libraryId: UUID): DetailedBookModel = transaction {
        val book = Book.findById(id) ?: throw ErrorResponse(HttpStatusCode.NotFound, "Could not find book")
        if (book.library.id.value != libraryId) throw ErrorResponse(HttpStatusCode.NotFound, "Could not find book")
        val tracks = Track.find { TTracks.book eq id }.orderBy(TTracks.trackNr to SortOrder.ASC).map { it.toModel() }
        DetailedBookModel.fromModel(book.toModel(), tracks)
    }

    override fun bookPosition(libraryId: UUID, id: UUID, order: SortOrder): Long = transaction {
        val book = book(id, libraryId)
        TBooks.select { TBooks.title.lowerCase() less book.title.lowercase() }
            .orderBy(TBooks.title.lowerCase() to order)
            .count()
    }

    override fun booksSorting(libraryId: UUID, order: SortOrder, limit: Int, offset: Long): List<UUID> = transaction {
        Book.find { TBooks.library eq libraryId }
            .orderBy(TBooks.title.lowerCase() to order)
            .limit(limit, offset)
            .map { it.id.value }
    }

    override fun search(query: String, libraryId: UUID): List<BookModel> = transaction {
        Book.find { TBooks.title like "%$query%" and (TBooks.library eq libraryId) }
            .limit(searchLimit)
            .map { it.toModel() }
    }

    override fun search(query: String): List<BookModel> = transaction {
        Book.find { TBooks.title like "%$query%" }.limit(searchLimit).map { it.toModel() }
    }

    override fun patchBook(id: UUID, libraryId: UUID, partialBook: PartialBookApiModel): BookModel = transaction {
        val book = Book.findById(id) ?: throw ErrorResponse.notFound("Book", id)
        if (book.library.id.value != libraryId) throw ErrorResponse.notFound("Book", id)
        book.apply {
            title = partialBook.title ?: title
            provider = partialBook.provider ?: provider
            providerID = partialBook.providerID ?: providerID
            providerRating = partialBook.providerRating ?: providerRating
            releaseDate = partialBook.releaseDate ?: releaseDate
            publisher = partialBook.publisher ?: publisher
            language = partialBook.language ?: language
            description = partialBook.description ?: description
            narrator = partialBook.narrator ?: narrator
            isbn = partialBook.isbn ?: isbn
            coverID = Image.getNewImage(partialBook.cover, currentImageID = coverID, default = coverID)
        }
        if (partialBook.authors != null) {
            book.authors =
                partialBook.authors
                    .map { Author.findById(it) ?: throw ErrorResponse.notFound("Author", id) }
                    .toSizedIterable()
        }
        if (partialBook.series != null) {
            book.series =
                partialBook.series
                    .map { Series.findById(it) ?: throw ErrorResponse.notFound("Series", id) }
                    .toSizedIterable()
        }
        book.toModel()
    }

    override fun replaceBook(id: UUID, libraryId: UUID, partialBook: BookApiModel): BookModel = transaction {
        val book = Book.findById(id) ?: throw ErrorResponse.notFound("Book", id)
        book.apply {
            title = partialBook.title
            provider = partialBook.provider
            providerID = partialBook.providerID
            providerRating = partialBook.providerRating
            releaseDate = partialBook.releaseDate
            publisher = partialBook.publisher
            language = partialBook.language
            description = partialBook.description
            narrator = partialBook.narrator
            isbn = partialBook.isbn
            coverID = Image.getNewImage(partialBook.cover, currentImageID = coverID, default = null)
            authors =
                partialBook.authors
                    .map { Author.findById(it) ?: throw ErrorResponse.notFound("Author", id) }
                    .toSizedIterable()
            series =
                partialBook.series
                    ?.map { Series.findById(it) ?: throw ErrorResponse.notFound("Series", id) }
                    ?.toSizedIterable()
                    ?: emptyList<Series>().toSizedIterable()
        }
        book.toModel()
    }
}
