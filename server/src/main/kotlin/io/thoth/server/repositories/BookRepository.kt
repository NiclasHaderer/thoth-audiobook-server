package io.thoth.server.repositories

import io.thoth.metadata.MetadataAgentWrapper
import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.responses.MetadataChapters
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.models.Book
import io.thoth.models.BookDetailed
import io.thoth.models.BookUpdate
import io.thoth.models.Chapter
import io.thoth.models.ChapterMark
import io.thoth.models.TitledId
import io.thoth.openapi.common.ifSet
import io.thoth.openapi.common.map
import io.thoth.openapi.common.orElse
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.ImageDownloader
import io.thoth.server.common.exposed.unless
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.common.extensions.naturalOrder
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.rows.BookRow
import io.thoth.server.database.rows.bookAuthors
import io.thoth.server.database.rows.bookSeries
import io.thoth.server.database.rows.booksToModels
import io.thoth.server.database.rows.toBookRow
import io.thoth.server.database.rows.toModel
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.BookAgentMetadataTable
import io.thoth.server.database.tables.BookField
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.BookMetadata
import io.thoth.server.database.tables.BookMetadataRow
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.TrackRow
import io.thoth.server.database.tables.TrackTable
import io.thoth.server.database.tables.authorIdsLinkedToBook
import io.thoth.server.database.tables.bookLayers
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.layer
import io.thoth.server.database.tables.replaceBookAuthors
import io.thoth.server.database.tables.replaceBookSeries
import io.thoth.server.database.tables.seriesIdsLinkedToBook
import io.thoth.server.database.tables.toTrackRow
import io.thoth.server.database.tables.write
import io.thoth.server.schedules.AutoMatchRequest
import io.thoth.server.schedules.AutoMatcher
import io.thoth.server.schedules.MatchableEntity
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import kotlin.math.abs

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
    private val autoMatcher by inject<AutoMatcher>()

    override fun total(
        libraryId: UUID,
        showInvisible: Boolean,
    ) = transaction {
        BookTable
            .selectAll()
            .where {
                (BookTable.library eq libraryId) and BookTable.deferDeletionUntil.isNull().unless(showInvisible)
            }.count()
    }

    override fun getAll(
        userId: UUID,
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
        showInvisible: Boolean,
    ): List<Book> =
        transaction {
            val rows =
                BookTable
                    .selectAll()
                    .where {
                        (BookTable.library eq libraryId) and BookTable.visible.unless(showInvisible)
                    }.orderBy(BookTable.title to order)
                    .offset(offset)
                    .limit(limit)
                    .map { it.toBookRow() }
            booksToModels(rows, userId)
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ): BookRow =
        transaction {
            BookTable
                .selectAll()
                .where { BookTable.id eq id and (BookTable.library eq libraryId) }
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
        userId: UUID,
        id: UUID,
        libraryId: UUID,
        showInvisible: Boolean,
    ): BookDetailed =
        transaction {
            val book = raw(id, libraryId)
            if (!showInvisible) requireVisible(BookTable, "Book", id)
            val ordered = orderedTracks(id)
            val tracks = ordered.map { (row, _) -> row }
            // The file layer never holds chapters, so a library preferring the files has to be honoured here. Only
            // markers count: one chapter per file is a fallback, not something the files say about the book.
            val filesWin =
                hasEmbeddedChapters(tracks) && BookUserMetadataTable.layer(id).chapters == null &&
                    libraryRepository.raw(libraryId).preferEmbeddedMetadata
            val marks = book.chapters?.takeUnless { filesWin } ?: fileChapterMarks(tracks)
            val bookRef = TitledId(book.id, book.title)
            BookDetailed.fromModel(
                book = book.toModel(userId),
                tracks = ordered.map { (row, trackNr) -> row.toModel(bookRef, trackNr) },
                chapters = buildChapters(marks, tracks),
                overridden = BookUserMetadataTable.layer(id).claimed.sorted(),
            )
        }

    context(_: Transaction)
    private fun orderedTracks(bookId: UUID): List<Pair<TrackRow, Int>> {
        val tracks =
            TrackTable
                .selectAll()
                .where { TrackTable.book eq bookId }
                .map { it.toTrackRow() }
        val numbered = tracks.mapNotNull { row -> row.trackNr?.let { row to it } }
        // Incomplete numbering cannot be trusted, so those books fall back to the file names and are numbered by position
        return if (numbered.size == tracks.size) {
            numbered.sortedBy { (_, trackNr) -> trackNr }
        } else {
            tracks.sortedWith(compareBy(naturalOrder) { it.path }).mapIndexed { index, row -> row to index + 1 }
        }
    }

    override fun search(
        userId: UUID,
        query: String,
        libraryId: UUID,
    ): List<Book> =
        transaction {
            val rows =
                BookTable
                    .selectAll()
                    .where {
                        matchesTitle(query) and (BookTable.library eq libraryId) and
                            BookTable.visible
                    }.orderBy(BookTable.title to SortOrder.ASC)
                    .limit(searchLimit)
                    .map { it.toBookRow() }
            booksToModels(rows, userId)
        }

    override fun modify(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
        partial: BookUpdate,
    ): Book {
        val currentCover = raw(id, libraryId).coverID
        val newCover =
            imageDownloader.download(partial.cover.orElse(null)?.takeUnless { it == currentCover?.toString() })
        return transaction {
            val edit = LayerEdit(bookLayers(id) ?: throw ErrorResponse.notFound("Book", id), partial.reset)
            val authors =
                edit.links(BookField.AUTHORS, partial.authors, bookAuthors(listOf(id))[id].orEmpty().map { it.id })
            val series =
                edit.links(BookField.SERIES, partial.series, bookSeries(listOf(id))[id].orEmpty().map { it.id })
            val cover = partial.cover.map { it?.let { getOrCreateImage(newCover, currentImageID = currentCover) } }
            partial.chapters.ifSet { marks ->
                val durationMs = orderedTracks(id).sumOf { (row, _) -> row.durationMs }
                if (marks.last().startMs >= durationMs) {
                    throw ErrorResponse.userError(
                        "The last chapter starts at ${marks.last().startMs}ms, " +
                            "but the book is only ${durationMs}ms long",
                    )
                }
            }
            BookUserMetadataTable.write(
                edit.user.copy(
                    title = edit.value(BookField.TITLE, partial.title) { title },
                    provider = edit.value(BookField.PROVIDER, partial.provider) { provider },
                    providerID = edit.value(BookField.PROVIDER_ID, partial.providerID) { providerID },
                    providerRating = edit.value(BookField.PROVIDER_RATING, partial.providerRating) { providerRating },
                    releaseDate = edit.value(BookField.RELEASE_DATE, partial.releaseDate) { releaseDate },
                    publisher = edit.value(BookField.PUBLISHER, partial.publisher) { publisher },
                    language = edit.value(BookField.LANGUAGE, partial.language) { language },
                    description = edit.value(BookField.DESCRIPTION, partial.description) { description },
                    narrators = edit.value(BookField.NARRATORS, partial.narrators) { narrators },
                    genres = edit.value(BookField.GENRES, partial.genres) { genres },
                    isbn = edit.value(BookField.ISBN, partial.isbn) { isbn },
                    coverID = edit.value(BookField.COVER_ID, cover) { coverID },
                    chapters = edit.value(BookField.CHAPTERS, partial.chapters) { chapters },
                    claimed = edit.claimed,
                ),
            )
            // Both ends of the change: whoever lost the book can be an orphan now, whoever gained it is not
            if (authors != null) {
                val authorIds = authors.map { authorRepository.raw(it, libraryId).id }
                val touched = authorIdsLinkedToBook(id) + authorIds
                replaceBookAuthors(id, MetadataLayer.USER, authorIds)
                refreshAuthorDeferral(touched)
            }
            if (series != null) {
                val seriesIds = series.map { seriesRepository.raw(it, libraryId).id }
                val touched = seriesIdsLinkedToBook(id) + seriesIds
                replaceBookSeries(id, MetadataLayer.USER, seriesIds.associateWith { null })
                refreshSeriesDeferral(touched)
            }
            raw(id, libraryId).toModel(userId)
        }
    }

    override fun create(
        bookName: String,
        libraryId: UUID,
        authors: List<UUID>,
        series: List<UUID>,
    ): BookRow =
        transaction {
            val id = BookTable.create(libraryRepository.raw(libraryId).id, bookName)
            BookFileMetadataTable.write(BookMetadataRow(book = id, title = bookName))
            replaceBookAuthors(id, MetadataLayer.FILE, authors)
            replaceBookSeries(id, MetadataLayer.FILE, series.associateWith { null })
            autoMatcher.matchOnCommit(AutoMatchRequest(MatchableEntity.BOOK, id, libraryId))
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
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): Book {
        val (metadataWrapper, bookName, region, authorName, language, narrator) =
            transaction {
                val book = raw(id, libraryId)
                val library = libraryRepository.raw(libraryId)
                AutoMatchQuery(
                    metadataAgents.forLibrary(library),
                    book.title,
                    library.region,
                    bookAuthors(listOf(id))[id].orEmpty().joinToString(", ") { it.name },
                    book.language ?: library.language,
                    book.narrators.firstOrNull(),
                )
            }

        val bookMetadata =
            runBlocking {
                metadataWrapper.bestBookMatch(
                    bookName = bookName,
                    region = region,
                    authorName = authorName,
                    narrator = narrator,
                    language = language,
                )
            } ?: throw noMatch(bookName)
        val matchedChapters =
            runBlocking { metadataWrapper.getBookChapters(bookMetadata.id.provider, bookMetadata.id.itemID, region) }

        val newCover = imageDownloader.download(bookMetadata.coverURL)
        // `bookMetadata.authors` and `.series` are deliberately dropped. Matching an entity updates that
        // entity's own details and nothing behind its foreign keys: resolving those names would either
        // rename a shared author, changing every other book by them, or mint a duplicate. Improving an
        // author or series name is its own match, against its own id.
        return transaction {
            val agent = BookAgentMetadataTable.layer(id)
            val durationMs = orderedTracks(id).sumOf { (row, _) -> row.durationMs }
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
                    narrators = bookMetadata.narrators.ifEmpty { null } ?: agent.narrators,
                    isbn = bookMetadata.isbn ?: agent.isbn,
                    coverID = getOrCreateImage(newCover, currentImageID = agent.coverID),
                    chapters = matchedChapters?.fitting(durationMs) ?: agent.chapters,
                ),
            )
            raw(id, libraryId).toModel(userId)
        }
    }

    private data class AutoMatchQuery(
        val metadataWrapper: MetadataAgentWrapper,
        val bookName: String,
        val region: MetadataRegion,
        val authorName: String,
        val language: MetadataLanguage,
        val narrator: String?,
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
    (BookTable innerJoin table)
        .select(BookTable.id)
        .where {
            val sameTitle = (table.title ilike pattern) and (BookTable.library eq libraryId)
            // An empty author list would make `inList` match nothing, so books without authors are
            // identified by title alone instead of never being found.
            if (authorIds.isEmpty()) {
                sameTitle
            } else {
                val booksOfAuthors =
                    AuthorBookTable
                        .select(AuthorBookTable.book)
                        .where {
                            (AuthorBookTable.author inList authorIds) and
                                (AuthorBookTable.addedBy eq MetadataLayer.FILE)
                        }
                sameTitle and (BookTable.id inSubQuery booksOfAuthors)
            }
        }.firstOrNull()
        ?.get(BookTable.id)
        ?.value

private fun matchesTitle(query: String): Op<Boolean> = BookTable.title ilike "%${escape(query)}%"

private const val CHAPTER_RUNTIME_TOLERANCE_MS = 30_000L

internal fun MetadataChapters.fitting(durationMs: Long): List<ChapterMark>? =
    chapters.takeIf { abs(runtimeMs - durationMs) <= CHAPTER_RUNTIME_TOLERANCE_MS }

internal fun hasEmbeddedChapters(tracks: List<TrackRow>): Boolean =
    tracks.isNotEmpty() && tracks.all { it.chapters.isNotEmpty() }

internal fun fileChapterMarks(tracks: List<TrackRow>): List<ChapterMark> {
    val useMarkers = hasEmbeddedChapters(tracks)
    val trackStarts = tracks.runningFold(0L) { start, track -> start + track.durationMs }
    return tracks.zip(trackStarts).flatMap { (track, trackStart) ->
        if (useMarkers) {
            track.chapters.sortedBy { it.startMs }.map { ChapterMark(it.title, trackStart + it.startMs) }
        } else {
            listOf(ChapterMark(track.title, trackStart))
        }
    }
}

// Marks are measured from the start of the book, so an edit made before the files changed can point past their end
internal fun buildChapters(
    marks: List<ChapterMark>,
    tracks: List<TrackRow>,
): List<Chapter> {
    val trackStarts = tracks.runningFold(0L) { start, track -> start + track.durationMs }
    val bookEnd = trackStarts.last()
    val inBook = marks.filter { it.startMs < bookEnd }
    return inBook.mapIndexed { index, mark ->
        Chapter(
            title = mark.title?.ifBlank { null },
            startMs = mark.startMs,
            endMs = inBook.getOrNull(index + 1)?.startMs ?: bookEnd,
            trackId = tracks[trackStarts.indexOfLast { it <= mark.startMs }].id,
        )
    }
}
