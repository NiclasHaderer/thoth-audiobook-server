package io.thoth.server.repositories

import io.thoth.metadata.MetadataProviders
import io.thoth.metadata.MetadataWrapper
import io.thoth.models.BookModel
import io.thoth.models.DetailedBookModel
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.api.BookApiModel
import io.thoth.server.api.PartialBookApiModel
import io.thoth.server.common.extensions.toSizedIterable
import io.thoth.server.database.access.getNewImage
import io.thoth.server.database.access.toModel
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookeEntity
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageEntity
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.database.tables.TracksTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

interface BookRepository : Repository<BookeEntity, BookModel, DetailedBookModel, PartialBookApiModel, BookApiModel> {
    fun findByName(
        bookTitle: String,
        authorIds: List<UUID>,
        libraryId: UUID,
    ): BookeEntity?

    fun getOrCreate(
        bookName: String,
        libraryId: UUID,
        authors: List<AuthorEntity>,
        series: List<SeriesEntity>,
    ): BookeEntity

    fun create(
        bookName: String,
        libraryId: UUID,
        authors: List<AuthorEntity>,
        series: List<SeriesEntity>,
    ): BookeEntity
}

class BookRepositoryImpl :
    BookRepository,
    KoinComponent {
    private val authorRepository by inject<AuthorRepository>()
    private val seriesRepository by inject<SeriesRepository>()
    private val libraryRepository by inject<LibraryRepository>()
    private val metadataProviders by inject<MetadataProviders>()

    override fun total(libraryId: UUID) = transaction { BookeEntity.find { BooksTable.library eq libraryId }.count() }

    override fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<BookModel> =
        transaction {
            BookeEntity
                .find { BooksTable.library eq libraryId }
                .orderBy(BooksTable.title.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it.toModel() }
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ): BookeEntity =
        transaction {
            BookeEntity.find { BooksTable.id eq id and (BooksTable.library eq libraryId) }.firstOrNull()
                ?: throw ErrorResponse.notFound("Book", id)
        }

    override fun findByName(
        bookTitle: String,
        authorIds: List<UUID>,
        libraryId: UUID,
    ): BookeEntity? =
        transaction {
            val rawBook =
                BooksTable
                    .join(AuthorBookTable, JoinType.INNER, BooksTable.id, AuthorBookTable.book)
                    .join(AuthorTable, JoinType.INNER, AuthorBookTable.authors, AuthorTable.id)
                    .selectAll()
                    .where {
                        (BooksTable.title like bookTitle) and
                            (AuthorBookTable.authors inList authorIds) and
                            (BooksTable.library eq libraryId)
                    }.firstOrNull() ?: return@transaction null
            BookeEntity.wrap(rawBook[BooksTable.id], rawBook)
        }

    override fun get(
        id: UUID,
        libraryId: UUID,
    ): DetailedBookModel =
        transaction {
            val book = raw(id, libraryId)
            val tracks =
                TrackEntity
                    .find { TracksTable.book eq id }
                    .orderBy(
                        TracksTable.trackNr to SortOrder.ASC,
                    ).map { it.toModel() }
            DetailedBookModel.fromModel(book.toModel(), tracks)
        }

    override fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
    ): Long =
        transaction {
            val book = get(id, libraryId)
            BooksTable
                .selectAll()
                .where {
                    BooksTable.title.lowerCase() less book.title.lowercase() and (BooksTable.library eq libraryId)
                }.orderBy(BooksTable.title.lowerCase() to order)
                .count()
        }

    override fun sorting(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<UUID> =
        transaction {
            BooksTable
                .selectAll()
                .where { BooksTable.library eq libraryId }
                .orderBy(BooksTable.title.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it[BooksTable.id].value }
        }

    override fun search(
        query: String,
        libraryId: UUID,
    ): List<BookModel> =
        transaction {
            BookeEntity
                .find { BooksTable.title like "%$query%" and (BooksTable.library eq libraryId) }
                .limit(searchLimit)
                .map { it.toModel() }
        }

    override fun search(query: String): List<BookModel> =
        transaction {
            BookeEntity.find { BooksTable.title like "%$query%" }.limit(searchLimit).map { it.toModel() }
        }

    override fun modify(
        id: UUID,
        libraryId: UUID,
        partial: PartialBookApiModel,
    ): BookModel =
        transaction {
            val book = raw(id, libraryId)
            book.apply {
                displayTitle = partial.title
                provider = partial.provider ?: provider
                providerID = partial.providerID ?: providerID
                providerRating = partial.providerRating ?: providerRating
                releaseDate = partial.releaseDate ?: releaseDate
                publisher = partial.publisher ?: publisher
                language = partial.language ?: language
                description = partial.description ?: description
                narrator = partial.narrator ?: narrator
                isbn = partial.isbn ?: isbn
                coverID = ImageEntity.getNewImage(partial.cover, currentImageID = coverID, default = coverID)
            }
            if (partial.authors != null) {
                book.authors = partial.authors.map { authorRepository.raw(it, libraryId) }.toSizedIterable()
            }
            if (partial.series != null) {
                book.series = partial.series.map { seriesRepository.raw(it, libraryId) }.toSizedIterable()
            }
            book.toModel()
        }

    override fun replace(
        id: UUID,
        libraryId: UUID,
        complete: BookApiModel,
    ): BookModel =
        transaction {
            val book = BookeEntity.findById(id) ?: throw ErrorResponse.notFound("Book", id)
            if (book.library.id.value !=
                libraryId
            ) {
                throw ErrorResponse.notFound("Book", id, "Book is not in that library")
            }
            book.apply {
                displayTitle = complete.title
                provider = complete.provider
                providerID = complete.providerID
                providerRating = complete.providerRating
                releaseDate = complete.releaseDate
                publisher = complete.publisher
                language = complete.language
                description = complete.description
                narrator = complete.narrator
                isbn = complete.isbn
                coverID = ImageEntity.getNewImage(complete.cover, currentImageID = coverID, default = null)
                authors = complete.authors.map { authorRepository.raw(it, libraryId) }.toSizedIterable()
                series =
                    complete.series?.map { seriesRepository.raw(it, libraryId) }?.toSizedIterable()
                        ?: emptyList<SeriesEntity>().toSizedIterable()
            }
            book.toModel()
        }

    override fun create(
        bookName: String,
        libraryId: UUID,
        authors: List<AuthorEntity>,
        series: List<SeriesEntity>,
    ): BookeEntity =
        transaction {
            BookeEntity
                .new {
                    title = bookName
                    this.authors = SizedCollection(authors)
                    this.series = SizedCollection(series)
                }.also { it.library = libraryRepository.raw(libraryId) }
        }

    override fun getOrCreate(
        bookName: String,
        libraryId: UUID,
        authors: List<AuthorEntity>,
        series: List<SeriesEntity>,
    ): BookeEntity =
        transaction {
            findByName(bookName, authors.map { it.id.value }, libraryId) ?: create(bookName, libraryId, authors, series)
        }

    override fun autoMatch(
        id: UUID,
        libraryId: UUID,
    ): BookModel =
        transaction {
            val book = raw(id, libraryId)
            val library = libraryRepository.raw(libraryId)

            val metadataAgents =
                library.metadataScanners.mapNotNull { agent -> metadataProviders.find { it.uniqueName == agent.name } }
            val metadataWrapper = MetadataWrapper(metadataAgents)

            val bookMetadata =
                runBlocking {
                    metadataWrapper
                        .getBookByName(
                            bookName = book.title,
                            region = library.language,
                            authorName = book.authors.joinToString(", ") { it.name },
                        ).firstOrNull()
                } ?: return@transaction book.toModel()

            modify(
                id,
                libraryId,
                PartialBookApiModel(
                    title = bookMetadata.title,
                    authors = null,
                    series = null,
                    provider = bookMetadata.id.provider,
                    providerID = bookMetadata.id.itemID,
                    providerRating = bookMetadata.providerRating,
                    releaseDate = bookMetadata.releaseDate,
                    publisher = bookMetadata.publisher,
                    language = bookMetadata.language,
                    description = bookMetadata.description,
                    narrator = bookMetadata.narrator,
                    isbn = bookMetadata.isbn,
                    cover = bookMetadata.coverURL,
                ),
            )
        }
}
