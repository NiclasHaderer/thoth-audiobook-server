package io.thoth.server.services

import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.models.BookModel
import io.thoth.models.DetailedBookModel
import io.thoth.server.api.BookApiModel
import io.thoth.server.api.PartialBookApiModel
import io.thoth.server.common.extensions.toSizedIterable
import io.thoth.server.database.access.getNewImage
import io.thoth.server.database.access.toModel
import io.thoth.server.database.tables.Book
import io.thoth.server.database.tables.Image
import io.thoth.server.database.tables.Series
import io.thoth.server.database.tables.TAuthorBookMapping
import io.thoth.server.database.tables.TAuthors
import io.thoth.server.database.tables.TBooks
import io.thoth.server.database.tables.TTracks
import io.thoth.server.database.tables.Track
import java.util.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface BookRepository : Repository<Book, BookModel, DetailedBookModel, PartialBookApiModel, BookApiModel> {
    fun findByName(bookTitle: String, authorId: UUID, libraryId: UUID): Book?
}

class BookRepositoryImpl(
    private val authorRepository: AuthorRepository,
    private val seriesRepository: SeriesRepository,
) : BookRepository {
    override fun total(libraryId: UUID) = transaction { Book.find { TBooks.library eq libraryId }.count() }

    override fun getAll(libraryId: UUID, order: SortOrder, limit: Int, offset: Long): List<BookModel> = transaction {
        Book.find { TBooks.library eq libraryId }
            .orderBy(TBooks.title.lowerCase() to order)
            .limit(limit, offset)
            .map { it.toModel() }
    }

    override fun raw(id: UUID, libraryId: UUID): Book = transaction {
        Book.find { TBooks.id eq id and (TBooks.library eq libraryId) }.firstOrNull()
            ?: throw ErrorResponse.notFound("Book", id)
    }

    override fun findByName(bookTitle: String, authorId: UUID, libraryId: UUID): Book? = transaction {
        val rawBook =
            TBooks.join(TAuthorBookMapping, JoinType.INNER, TBooks.id, TAuthorBookMapping.book)
                .join(TAuthors, JoinType.INNER, TAuthorBookMapping.author, TAuthors.id)
                .select {
                    (TBooks.title like bookTitle) and
                        (TAuthorBookMapping.author eq authorId) and
                        (TBooks.library eq libraryId)
                }
                .firstOrNull()
                ?: return@transaction null
        Book.wrap(rawBook[TBooks.id], rawBook)
    }

    override fun get(id: UUID, libraryId: UUID): DetailedBookModel = transaction {
        val book = raw(id, libraryId)
        val tracks = Track.find { TTracks.book eq id }.orderBy(TTracks.trackNr to SortOrder.ASC).map { it.toModel() }
        DetailedBookModel.fromModel(book.toModel(), tracks)
    }

    override fun position(id: UUID, libraryId: UUID, order: SortOrder): Long = transaction {
        val book = get(id, libraryId)
        TBooks.select { TBooks.title.lowerCase() less book.title.lowercase() and (TBooks.library eq libraryId) }
            .orderBy(TBooks.title.lowerCase() to order)
            .count()
    }

    override fun sorting(libraryId: UUID, order: SortOrder, limit: Int, offset: Long): List<UUID> = transaction {
        TBooks.select { TBooks.library eq libraryId }
            .orderBy(TBooks.title.lowerCase() to order)
            .limit(limit, offset)
            .map { it[TBooks.id].value }
    }

    override fun search(query: String, libraryId: UUID): List<BookModel> = transaction {
        Book.find { TBooks.title like "%$query%" and (TBooks.library eq libraryId) }
            .limit(searchLimit)
            .map { it.toModel() }
    }

    override fun search(query: String): List<BookModel> = transaction {
        Book.find { TBooks.title like "%$query%" }.limit(searchLimit).map { it.toModel() }
    }

    override fun modify(id: UUID, libraryId: UUID, partial: PartialBookApiModel): BookModel = transaction {
        val book = raw(id, libraryId)
        book.apply {
            title = partial.title ?: title
            provider = partial.provider ?: provider
            providerID = partial.providerID ?: providerID
            providerRating = partial.providerRating ?: providerRating
            releaseDate = partial.releaseDate ?: releaseDate
            publisher = partial.publisher ?: publisher
            language = partial.language ?: language
            description = partial.description ?: description
            narrator = partial.narrator ?: narrator
            isbn = partial.isbn ?: isbn
            coverID = Image.getNewImage(partial.cover, currentImageID = coverID, default = coverID)
        }
        if (partial.authors != null) {
            book.authors = partial.authors.map { authorRepository.raw(it, libraryId) }.toSizedIterable()
        }
        if (partial.series != null) {
            book.series = partial.series.map { seriesRepository.raw(it, libraryId) }.toSizedIterable()
        }
        book.toModel()
    }

    override fun replace(id: UUID, libraryId: UUID, complete: BookApiModel): BookModel = transaction {
        val book = Book.findById(id) ?: throw ErrorResponse.notFound("Book", id)
        if (book.library.id.value != libraryId) throw ErrorResponse.notFound("Book", id, "Book is not in that library")
        book.apply {
            title = complete.title
            provider = complete.provider
            providerID = complete.providerID
            providerRating = complete.providerRating
            releaseDate = complete.releaseDate
            publisher = complete.publisher
            language = complete.language
            description = complete.description
            narrator = complete.narrator
            isbn = complete.isbn
            coverID = Image.getNewImage(complete.cover, currentImageID = coverID, default = null)
            authors = complete.authors.map { authorRepository.raw(it, libraryId) }.toSizedIterable()
            series =
                complete.series?.map { seriesRepository.raw(it, libraryId) }?.toSizedIterable()
                    ?: emptyList<Series>().toSizedIterable()
        }
        book.toModel()
    }
}
