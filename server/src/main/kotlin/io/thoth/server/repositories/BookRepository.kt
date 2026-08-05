package io.thoth.server.repositories

import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.models.Book
import io.thoth.models.BookDetailed
import io.thoth.models.BookUpdate
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.common.extensions.naturalOrder
import io.thoth.server.common.extensions.toSizedIterable
import io.thoth.server.database.access.fetchImage
import io.thoth.server.database.access.getNewImage
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageEntity
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.database.tables.TracksTable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

interface BookRepository : Repository<BookEntity, Book, BookDetailed, BookUpdate> {
    fun findByName(
        bookTitle: String,
        authorIds: List<UUID>,
        libraryId: UUID,
    ): BookEntity?

    fun getOrCreate(
        bookName: String,
        libraryId: UUID,
        authors: List<AuthorEntity>,
        series: List<SeriesEntity>,
    ): BookEntity

    fun create(
        bookName: String,
        libraryId: UUID,
        authors: List<AuthorEntity>,
        series: List<SeriesEntity>,
    ): BookEntity
}

class BookRepositoryImpl :
    BookRepository,
    KoinComponent {
    private val authorRepository by inject<AuthorRepository>()
    private val seriesRepository by inject<SeriesRepository>()
    private val libraryRepository by inject<LibraryRepository>()
    private val metadataAgents by inject<MetadataAgents>()

    override fun total(libraryId: UUID) = transaction { BookEntity.find { BooksTable.library eq libraryId }.count() }

    override fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<Book> =
        transaction {
            BookEntity
                .find { BooksTable.library eq libraryId }
                .orderBy(BooksTable.displayedTitle.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .withRelations()
                .map { it.toModel() }
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ): BookEntity =
        transaction {
            BookEntity.find { BooksTable.id eq id and (BooksTable.library eq libraryId) }.firstOrNull()
                ?: throw ErrorResponse.notFound("Book", id)
        }

    override fun findByName(
        bookTitle: String,
        authorIds: List<UUID>,
        libraryId: UUID,
    ): BookEntity? =
        transaction {
            BookEntity
                .find {
                    val sameTitle = titledExactly(bookTitle) and (BooksTable.library eq libraryId)
                    // An empty author list would make `inList` match nothing, so books without authors are
                    // identified by title alone instead of never being found.
                    if (authorIds.isEmpty()) {
                        sameTitle
                    } else {
                        val booksOfAuthors =
                            AuthorBookTable
                                .select(AuthorBookTable.book)
                                .where { AuthorBookTable.authors inList authorIds }
                        sameTitle and (BooksTable.id inSubQuery booksOfAuthors)
                    }
                }.firstOrNull()
        }

    override fun get(
        id: UUID,
        libraryId: UUID,
    ): BookDetailed =
        transaction {
            val book = raw(id, libraryId)
            val tracks = TrackEntity.find { TracksTable.book eq id }.toList()
            // Incomplete numbering cannot be trusted, so those books fall back to the file names
            val ordered =
                if (tracks.any { it.trackNr == null }) {
                    tracks.sortedWith(compareBy(naturalOrder) { it.path })
                } else {
                    tracks.sortedBy { it.trackNr }
                }
            BookDetailed.fromModel(book.toModel(), ordered.map { it.toModel() })
        }

    override fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
    ): Long =
        transaction {
            val title = raw(id, libraryId).displayedTitle.lowercase()
            BooksTable
                .selectAll()
                .where {
                    val precedes =
                        if (order == SortOrder.ASC) {
                            BooksTable.displayedTitle.lowerCase() less title
                        } else {
                            BooksTable.displayedTitle.lowerCase() greater title
                        }
                    precedes and (BooksTable.library eq libraryId)
                }.count()
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
                .orderBy(BooksTable.displayedTitle.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it[BooksTable.id].value }
        }

    override fun search(
        query: String,
        libraryId: UUID,
    ): List<Book> =
        transaction {
            BookEntity
                .find { matchesTitle(query) and (BooksTable.library eq libraryId) }
                .orderBy(BooksTable.displayedTitle.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .withRelations()
                .map { it.toModel() }
        }

    override fun search(query: String): List<Book> =
        transaction {
            BookEntity
                .find { matchesTitle(query) }
                .orderBy(BooksTable.displayedTitle.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .withRelations()
                .map { it.toModel() }
        }

    override fun modify(
        id: UUID,
        libraryId: UUID,
        partial: BookUpdate,
    ): Book {
        val newCover = fetchImage(partial.cover)
        return transaction {
            val book = raw(id, libraryId)
            book.apply {
                displayTitle = partial.title ?: displayTitle
                provider = partial.provider ?: provider
                providerID = partial.providerID ?: providerID
                providerRating = partial.providerRating ?: providerRating
                releaseDate = partial.releaseDate ?: releaseDate
                publisher = partial.publisher ?: publisher
                language = partial.language ?: language
                description = partial.description ?: description
                narrator = partial.narrator ?: narrator
                isbn = partial.isbn ?: isbn
                coverID = ImageEntity.getNewImage(newCover, currentImageID = coverID, default = coverID)
            }
            if (partial.authors != null) {
                book.authors = partial.authors.map { authorRepository.raw(it, libraryId) }.toSizedIterable()
            }
            if (partial.series != null) {
                book.series = partial.series.map { seriesRepository.raw(it, libraryId) }.toSizedIterable()
            }
            book.toModel()
        }
    }

    override fun create(
        bookName: String,
        libraryId: UUID,
        authors: List<AuthorEntity>,
        series: List<SeriesEntity>,
    ): BookEntity =
        transaction {
            BookEntity
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
    ): BookEntity =
        transaction {
            findByName(bookName, authors.map { it.id.value }, libraryId) ?: create(bookName, libraryId, authors, series)
        }

    override fun autoMatch(
        id: UUID,
        libraryId: UUID,
    ): Book {
        val (metadataWrapper, bookName, region, authorName) =
            transaction {
                val book = raw(id, libraryId)
                val library = libraryRepository.raw(libraryId)
                AutoMatchQuery(
                    metadataAgents.forLibrary(library),
                    book.displayedTitle,
                    library.language,
                    book.authors.joinToString(", ") { it.displayedName },
                )
            }

        val bookMetadata =
            runBlocking {
                metadataWrapper.getBookByName(bookName = bookName, region = region, authorName = authorName)
                    .firstOrNull()
            } ?: return transaction { raw(id, libraryId).toModel() }

        return modify(
            id,
            libraryId,
            BookUpdate(
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

    private data class AutoMatchQuery(
        val metadataWrapper: MetadataAgent,
        val bookName: String,
        val region: String,
        val authorName: String,
    )
}

private fun <T : Iterable<BookEntity>> T.withRelations(): T =
    with(BookEntity::authors, BookEntity::series, BookEntity::genres)

// Same split as authors: title keeps tracking the files, displayTitle holds a rename, both stay matchable.
private fun titledExactly(title: String): Op<Boolean> = eitherTitle(escape(title))

private fun matchesTitle(query: String): Op<Boolean> = eitherTitle("%${escape(query)}%")

private fun eitherTitle(pattern: String): Op<Boolean> =
    (BooksTable.title ilike pattern) or (BooksTable.displayTitle ilike pattern)
