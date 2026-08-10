package io.thoth.server.repositories

import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.models.Book
import io.thoth.models.BookDetailed
import io.thoth.models.BookUpdate
import io.thoth.models.TitledId
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.ImageDownloader
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.common.extensions.naturalOrder
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.BookAgentMetadataTable
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.BookMetadata
import io.thoth.server.database.tables.BookMetadataRow
import io.thoth.server.database.views.BookRow
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.views.bookAuthors
import io.thoth.server.database.views.booksToModels
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.layer
import io.thoth.server.database.tables.replaceBookAuthors
import io.thoth.server.database.tables.replaceBookSeries
import io.thoth.server.database.views.toBookRow
import io.thoth.server.database.views.toModel
import io.thoth.server.database.tables.toTrackRow
import io.thoth.server.database.tables.write
import io.thoth.server.database.views.BookMetadataView
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
    fun findByTaggedName(
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
                BookMetadataView
                    .selectAll()
                    .where { BookMetadataView.library eq libraryId }
                    .orderBy(BookMetadataView.title.lowerCase() to order)
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
            BookMetadataView
                .selectAll()
                .where { BookMetadataView.id eq id and (BookMetadataView.library eq libraryId) }
                .firstOrNull()
                ?.toBookRow()
                ?: throw ErrorResponse.notFound("Book", id)
        }

    override fun findByTaggedName(
        bookTitle: String,
        authorIds: List<UUID>,
        libraryId: UUID,
    ): BookRow? =
        transaction {
            idOfTaggedName(escape(bookTitle), authorIds, libraryId)?.let { raw(it, libraryId) }
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
            val bookRef = TitledId(book.id, book.title)
            BookDetailed.fromModel(book.toModel(), ordered.map { it.toModel(bookRef) })
        }

    override fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
    ): Long =
        transaction {
            val title = raw(id, libraryId).title.lowercase()
            BookMetadataView
                .selectAll()
                .where {
                    val precedes =
                        if (order == SortOrder.ASC) {
                            BookMetadataView.title.lowerCase() less title
                        } else {
                            BookMetadataView.title.lowerCase() greater title
                        }
                    precedes and (BookMetadataView.library eq libraryId)
                }.count()
        }

    override fun sorting(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<UUID> =
        transaction {
            BookMetadataView
                .selectAll()
                .where { BookMetadataView.library eq libraryId }
                .orderBy(BookMetadataView.title.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it[BookMetadataView.id] }
        }

    override fun search(
        query: String,
        libraryId: UUID,
    ): List<Book> =
        transaction {
            val rows =
                BookMetadataView
                    .selectAll()
                    .where { matchesTitle(query) and (BookMetadataView.library eq libraryId) }
                    .orderBy(BookMetadataView.title.lowerCase() to SortOrder.ASC)
                    .limit(searchLimit)
                    .map { it.toBookRow() }
            booksToModels(rows)
        }

    override fun search(query: String): List<Book> =
        transaction {
            val rows =
                BookMetadataView
                    .selectAll()
                    .where { matchesTitle(query) }
                    .orderBy(BookMetadataView.title.lowerCase() to SortOrder.ASC)
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
            val user = BookUserMetadataTable.layer(id)
            BookUserMetadataTable.write(
                user.copy(
                    title = partial.title ?: user.title,
                    provider = partial.provider ?: user.provider,
                    providerID = partial.providerID ?: user.providerID,
                    providerRating = partial.providerRating ?: user.providerRating,
                    releaseDate = partial.releaseDate ?: user.releaseDate,
                    publisher = partial.publisher ?: user.publisher,
                    language = partial.language ?: user.language,
                    description = partial.description ?: user.description,
                    narrator = partial.narrator ?: user.narrator,
                    isbn = partial.isbn ?: user.isbn,
                    coverID = getOrCreateImage(newCover, currentImageID = user.coverID),
                    authorsSet = user.authorsSet || partial.authors != null,
                    seriesSet = user.seriesSet || partial.series != null,
                ),
            )
            if (partial.authors != null) {
                val authorIds = partial.authors.map { authorRepository.raw(it, libraryId).id }
                replaceBookAuthors(id, MetadataLayer.USER, authorIds)
            }
            if (partial.series != null) {
                val seriesIds = partial.series.map { seriesRepository.raw(it, libraryId).id }
                replaceBookSeries(id, MetadataLayer.USER, seriesIds.associateWith { null })
            }
            raw(id, libraryId).toModel()
        }
    }

    override fun create(
        bookName: String,
        libraryId: UUID,
        authors: List<UUID>,
        series: List<UUID>,
    ): BookRow =
        transaction {
            val id = BooksTable.create(libraryRepository.raw(libraryId).id)
            BookFileMetadataTable.write(BookMetadataRow(book = id, title = bookName))
            replaceBookAuthors(id, MetadataLayer.FILE, authors)
            replaceBookSeries(id, MetadataLayer.FILE, series.associateWith { null })
            raw(id, libraryId)
        }

    override fun getOrCreate(
        bookName: String,
        libraryId: UUID,
        authors: List<UUID>,
        series: List<UUID>,
    ): BookRow =
        transaction {
            findByTaggedName(bookName, authors, libraryId) ?: create(bookName, libraryId, authors, series)
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
                    book.title,
                    library.language,
                    bookAuthors(listOf(id))[id].orEmpty().joinToString(", ") { it.name },
                )
            }

        val bookMetadata =
            runBlocking {
                metadataWrapper.getBookByName(bookName = bookName, region = region, authorName = authorName)
                    .firstOrNull()
            } ?: return transaction { raw(id, libraryId).toModel() }

        val newCover = imageDownloader.download(bookMetadata.coverURL)
        // `bookMetadata.authors` and `.series` are deliberately dropped. Matching an entity updates that
        // entity's own details and nothing behind its foreign keys: resolving those names would either
        // rename a shared author, changing every other book by them, or mint a duplicate. Improving an
        // author or series name is its own match, against its own id.
        return transaction {
            val agent = BookAgentMetadataTable.layer(id)
            BookAgentMetadataTable.write(
                agent.copy(
                    title = bookMetadata.title ?: agent.title,
                    provider = bookMetadata.id.provider,
                    providerID = bookMetadata.id.itemID,
                    providerRating = bookMetadata.providerRating ?: agent.providerRating,
                    releaseDate = bookMetadata.releaseDate ?: agent.releaseDate,
                    publisher = bookMetadata.publisher ?: agent.publisher,
                    language = bookMetadata.language ?: agent.language,
                    description = bookMetadata.description ?: agent.description,
                    narrator = bookMetadata.narrator ?: agent.narrator,
                    isbn = bookMetadata.isbn ?: agent.isbn,
                    coverID = getOrCreateImage(newCover, currentImageID = agent.coverID),
                ),
            )
            raw(id, libraryId).toModel()
        }
    }

    private data class AutoMatchQuery(
        val metadataWrapper: MetadataAgent,
        val bookName: String,
        val region: String,
        val authorName: String,
    )
}

context(_: Transaction)
private fun idOfTaggedName(
    pattern: String,
    authorIds: List<UUID>,
    libraryId: UUID,
): UUID? =
    idInLayer(BookFileMetadataTable, pattern, authorIds, libraryId)
        ?: idInLayer(BookUserMetadataTable, pattern, authorIds, libraryId)
        ?: idInLayer(BookAgentMetadataTable, pattern, authorIds, libraryId)

context(_: Transaction)
private fun idInLayer(
    table: BookMetadata,
    pattern: String,
    authorIds: List<UUID>,
    libraryId: UUID,
): UUID? =
    (BooksTable innerJoin table)
        .select(BooksTable.id)
        .where {
            val sameTitle = (table.title ilike pattern) and (BooksTable.library eq libraryId)
            // An empty author list would make `inList` match nothing, so books without authors are
            // identified by title alone instead of never being found.
            if (authorIds.isEmpty()) {
                sameTitle
            } else {
                val booksOfAuthors =
                    AuthorBookTable
                        .select(AuthorBookTable.book)
                        .where {
                            (AuthorBookTable.authors inList authorIds) and
                                (AuthorBookTable.addedBy eq MetadataLayer.FILE)
                        }
                sameTitle and (BooksTable.id inSubQuery booksOfAuthors)
            }
        }.firstOrNull()
        ?.get(BooksTable.id)
        ?.value

private fun matchesTitle(query: String): Op<Boolean> =
    BookMetadataView.title ilike "%${escape(query)}%"
