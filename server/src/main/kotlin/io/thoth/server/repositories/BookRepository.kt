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
import io.thoth.server.database.rows.update
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.BookField
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.TrackRow
import io.thoth.server.database.tables.TrackTable
import io.thoth.server.database.tables.authorIdsLinkedToBook
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.replaceBookAuthors
import io.thoth.server.database.tables.replaceBookSeries
import io.thoth.server.database.tables.seriesIdsLinkedToBook
import io.thoth.server.database.tables.toTrackRow
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
            val marks = book.chapters ?: fileChapterMarks(tracks)
            val bookRef = TitledId(book.id, book.title)
            BookDetailed.fromModel(
                book = book.toModel(userId),
                tracks = ordered.map { (row, trackNr) -> row.toModel(bookRef, trackNr) },
                chapters = buildChapters(marks, tracks),
                locked = book.locked.sorted(),
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
            val book = raw(id, libraryId)
            val edit = UserEdit(book.locked, partial.unlock)
            val authors = edit.links(BookField.AUTHORS, authorIdsLinkedToBook(id), partial.authors)
            val series = edit.links(BookField.SERIES, seriesIdsLinkedToBook(id), partial.series)
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
            // Unlike the other fields, unlocked chapters have no value to keep: what the files say is not stored,
            // so the user's chapters have to go for the files' to come back
            val unlocksChapters =
                BookField.CHAPTERS in book.locked && BookField.CHAPTERS in partial.unlock.orElse(emptyList())
            val chapters = edit.value(BookField.CHAPTERS, book.chapters, partial.chapters)
            BookTable.update(
                book.copy(
                    title = edit.value(BookField.TITLE, book.title, partial.title),
                    provider = edit.value(BookField.PROVIDER, book.provider, partial.provider),
                    providerID = edit.value(BookField.PROVIDER_ID, book.providerID, partial.providerID),
                    providerRating = edit.value(BookField.PROVIDER_RATING, book.providerRating, partial.providerRating),
                    releaseDate = edit.value(BookField.RELEASE_DATE, book.releaseDate, partial.releaseDate),
                    publisher = edit.value(BookField.PUBLISHER, book.publisher, partial.publisher),
                    language = edit.value(BookField.LANGUAGE, book.language, partial.language),
                    description = edit.value(BookField.DESCRIPTION, book.description, partial.description),
                    narrators = edit.value(BookField.NARRATORS, book.narrators, partial.narrators),
                    genres = edit.value(BookField.GENRES, book.genres, partial.genres),
                    isbn = edit.value(BookField.ISBN, book.isbn, partial.isbn),
                    coverID = edit.value(BookField.COVER_ID, book.coverID, cover),
                    chapters = chapters.takeUnless { unlocksChapters },
                    locked = edit.locked,
                ),
            )
            // Both ends of the change: whoever lost the book can be an orphan now, whoever gained it is not
            if (authors != null) {
                val authorIds = authors.map { authorRepository.raw(it, libraryId).id }
                val touched = authorIdsLinkedToBook(id) + authorIds
                replaceBookAuthors(id, authorIds)
                refreshAuthorDeferral(touched)
            }
            if (series != null) {
                val seriesIds = series.map { seriesRepository.raw(it, libraryId).id }
                val touched = seriesIdsLinkedToBook(id) + seriesIds
                replaceBookSeries(id, seriesIds.associateWith { null })
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
            val id = BookTable.create(libraryRepository.raw(libraryId).id, bookName, taggedName = bookName)
            replaceBookAuthors(id, authors)
            replaceBookSeries(id, series.associateWith { null })
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
        val (metadataWrapper, bookName, region, authorName, language, narrator, preferFile) =
            transaction {
                val book = raw(id, libraryId)
                val library = libraryRepository.raw(libraryId)
                AutoMatchQuery(
                    metadataAgents.forLibrary(library),
                    book.title,
                    library.region,
                    bookAuthors(listOf(id))[id].orEmpty().joinToString(", ") { it.name },
                    book.language ?: library.language,
                    book.narrators?.firstOrNull(),
                    library.preferEmbeddedMetadata,
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
            val book = raw(id, libraryId)
            val tracks = orderedTracks(id).map { (row, _) -> row }
            val durationMs = tracks.sumOf { it.durationMs }
            // A scan never stores chapters, so a library preferring the files is honoured by not storing the
            // agent's. Only markers count: one chapter per file is a fallback, not something the files say about
            // the book.
            val filesWin = preferFile && hasEmbeddedChapters(tracks)
            val write = AutomaticWrite(book.locked, onlyFillEmpty = preferFile)
            BookTable.update(
                book.copy(
                    title = write.value(BookField.TITLE, book.title) { bookMetadata.title },
                    provider = write.overwrite(BookField.PROVIDER, book.provider) { bookMetadata.id.provider },
                    providerID = write.overwrite(BookField.PROVIDER_ID, book.providerID) { bookMetadata.id.itemID },
                    providerRating =
                        write.value(BookField.PROVIDER_RATING, book.providerRating) { bookMetadata.providerRating },
                    releaseDate = write.value(BookField.RELEASE_DATE, book.releaseDate) { bookMetadata.releaseDate },
                    publisher = write.value(BookField.PUBLISHER, book.publisher) { bookMetadata.publisher },
                    language = write.value(BookField.LANGUAGE, book.language) { bookMetadata.language },
                    description = write.value(BookField.DESCRIPTION, book.description) { bookMetadata.description },
                    narrators =
                        write.value(BookField.NARRATORS, book.narrators) { bookMetadata.narrators.ifEmpty { null } },
                    isbn = write.value(BookField.ISBN, book.isbn) { bookMetadata.isbn },
                    coverID = write.value(BookField.COVER_ID, book.coverID) { getOrCreateImage(newCover, it) },
                    chapters =
                        write.value(BookField.CHAPTERS, book.chapters) {
                            matchedChapters?.fitting(durationMs)?.takeUnless { filesWin }
                        },
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
        val preferFile: Boolean,
    )
}

// The current title or the one the files use, by one of the tagged authors. Any link counts: a user may have
// edited the authors since, but a book that shares none of them is somebody else's book of the same name.
context(_: Transaction)
private fun idOfTaggedName(
    pattern: String,
    authorIds: List<UUID>,
    libraryId: UUID,
): UUID? =
    BookTable
        .select(BookTable.id)
        .where {
            val sameTitle =
                ((BookTable.title ilike pattern) or (BookTable.taggedName ilike pattern)) and
                    (BookTable.library eq libraryId)
            // An empty author list would make `inList` match nothing, so books without authors are
            // identified by title alone instead of never being found.
            if (authorIds.isEmpty()) {
                sameTitle
            } else {
                val booksOfAuthors =
                    AuthorBookTable
                        .select(AuthorBookTable.book)
                        .where { AuthorBookTable.author inList authorIds }
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
            title = mark.title,
            startMs = mark.startMs,
            endMs = inBook.getOrNull(index + 1)?.startMs ?: bookEnd,
            trackId = tracks[trackStarts.indexOfLast { it <= mark.startMs }].id,
        )
    }
}
