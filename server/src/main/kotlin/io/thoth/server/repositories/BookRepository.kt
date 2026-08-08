package io.thoth.server.repositories

import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.models.Book
import io.thoth.models.BookDetailed
import io.thoth.models.BookUpdate
import io.thoth.models.TitledId
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.common.ImageDownloader
import io.thoth.server.common.extensions.naturalOrder
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.BookRow
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.bookAuthors
import io.thoth.server.database.tables.booksToModels
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.replaceLinks
import io.thoth.server.database.tables.toBookRow
import io.thoth.server.database.tables.toModel
import io.thoth.server.database.tables.toTrackRow
import io.thoth.server.database.tables.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

interface BookRepository : Repository<BookRow, Book, BookDetailed, BookUpdate> {
    fun findByName(
        bookTitle: String,
        authorIds: List<UUID>,
        libraryId: UUID,
    ): BookRow?

    fun getOrCreate(
        bookName: String,
        libraryId: UUID,
        authors: List<UUID>,
        series: List<UUID>,
    ): BookRow

    fun create(
        bookName: String,
        libraryId: UUID,
        authors: List<UUID>,
        series: List<UUID>,
    ): BookRow
}

class BookRepositoryImpl :
    BookRepository,
    KoinComponent {
    private val authorRepository by inject<AuthorRepository>()
    private val seriesRepository by inject<SeriesRepository>()
    private val libraryRepository by inject<LibraryRepository>()
    private val metadataAgents by inject<MetadataAgents>()
    private val imageDownloader by inject<ImageDownloader>()

    override fun total(libraryId: UUID) =
        transaction { BooksTable.selectAll().where { BooksTable.library eq libraryId }.count() }

    override fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<Book> =
        transaction {
            val rows =
                BooksTable
                    .selectAll()
                    .where { BooksTable.library eq libraryId }
                    .orderBy(BooksTable.displayedTitle.lowerCase() to order)
                    .offset(offset)
                    .limit(limit)
                    .map { it.toBookRow() }
            booksToModels(rows)
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ): BookRow =
        transaction {
            BooksTable
                .selectAll()
                .where { BooksTable.id eq id and (BooksTable.library eq libraryId) }
                .firstOrNull()
                ?.toBookRow()
                ?: throw ErrorResponse.notFound("Book", id)
        }

    override fun findByName(
        bookTitle: String,
        authorIds: List<UUID>,
        libraryId: UUID,
    ): BookRow? =
        transaction {
            BooksTable
                .selectAll()
                .where {
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
                ?.toBookRow()
        }

    override fun get(
        id: UUID,
        libraryId: UUID,
    ): BookDetailed =
        transaction {
            val book = raw(id, libraryId)
            val tracks =
                TracksTable
                    .selectAll()
                    .where { TracksTable.book eq id }
                    .map { it.toTrackRow() }
            // Incomplete numbering cannot be trusted, so those books fall back to the file names
            val ordered =
                if (tracks.any { it.trackNr == null }) {
                    tracks.sortedWith(compareBy(naturalOrder) { it.path })
                } else {
                    tracks.sortedBy { it.trackNr }
                }
            val bookRef = TitledId(book.id, book.displayedTitle)
            BookDetailed.fromModel(book.toModel(), ordered.map { it.toModel(bookRef) })
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
            val rows =
                BooksTable
                    .selectAll()
                    .where { matchesTitle(query) and (BooksTable.library eq libraryId) }
                    .orderBy(BooksTable.displayedTitle.lowerCase() to SortOrder.ASC)
                    .limit(searchLimit)
                    .map { it.toBookRow() }
            booksToModels(rows)
        }

    override fun search(query: String): List<Book> =
        transaction {
            val rows =
                BooksTable
                    .selectAll()
                    .where { matchesTitle(query) }
                    .orderBy(BooksTable.displayedTitle.lowerCase() to SortOrder.ASC)
                    .limit(searchLimit)
                    .map { it.toBookRow() }
            booksToModels(rows)
        }

    override fun modify(
        id: UUID,
        libraryId: UUID,
        partial: BookUpdate,
    ): Book {
        val currentCover = raw(id, libraryId).coverID
        val newCover = imageDownloader.download(partial.cover?.takeUnless { it == currentCover?.toString() })
        return transaction {
            val book = raw(id, libraryId)
            val updated =
                book.copy(
                    displayTitle = partial.title ?: book.displayTitle,
                    provider = partial.provider ?: book.provider,
                    providerID = partial.providerID ?: book.providerID,
                    providerRating = partial.providerRating ?: book.providerRating,
                    releaseDate = partial.releaseDate ?: book.releaseDate,
                    publisher = partial.publisher ?: book.publisher,
                    language = partial.language ?: book.language,
                    description = partial.description ?: book.description,
                    narrator = partial.narrator ?: book.narrator,
                    isbn = partial.isbn ?: book.isbn,
                    coverID = getOrCreateImage(newCover, currentImageID = book.coverID),
                )
            BooksTable.update(updated)
            if (partial.authors != null) {
                val authorIds = partial.authors.map { authorRepository.raw(it, libraryId).id }
                AuthorBookTable.replaceLinks(AuthorBookTable.book, id, AuthorBookTable.authors, authorIds)
            }
            if (partial.series != null) {
                val seriesIds = partial.series.map { seriesRepository.raw(it, libraryId).id }
                SeriesBookTable.replaceLinks(SeriesBookTable.book, id, SeriesBookTable.series, seriesIds)
            }
            updated.toModel()
        }
    }

    override fun create(
        bookName: String,
        libraryId: UUID,
        authors: List<UUID>,
        series: List<UUID>,
    ): BookRow =
        transaction {
            val row =
                BookRow(
                    id = UUID.randomUUID(),
                    title = bookName,
                    displayTitle = null,
                    releaseDate = null,
                    publisher = null,
                    language = null,
                    description = null,
                    narrator = null,
                    isbn = null,
                    provider = null,
                    providerID = null,
                    providerRating = null,
                    coverID = null,
                    library = libraryRepository.raw(libraryId).id,
                )
            BooksTable.insert(row)
            AuthorBookTable.replaceLinks(AuthorBookTable.book, row.id, AuthorBookTable.authors, authors)
            SeriesBookTable.replaceLinks(SeriesBookTable.book, row.id, SeriesBookTable.series, series)
            row
        }

    override fun getOrCreate(
        bookName: String,
        libraryId: UUID,
        authors: List<UUID>,
        series: List<UUID>,
    ): BookRow =
        transaction {
            findByName(bookName, authors, libraryId) ?: create(bookName, libraryId, authors, series)
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
                    bookAuthors(listOf(id))[id].orEmpty().joinToString(", ") { it.name },
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

// Same split as authors: title keeps tracking the files, displayTitle holds a rename, both stay matchable.
private fun titledExactly(title: String): Op<Boolean> = eitherTitle(escape(title))

private fun matchesTitle(query: String): Op<Boolean> = eitherTitle("%${escape(query)}%")

private fun eitherTitle(pattern: String): Op<Boolean> =
    (BooksTable.title ilike pattern) or (BooksTable.displayTitle ilike pattern)
