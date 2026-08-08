package io.thoth.server.file

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.server.common.extensions.canonicalString
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.access.getOrCreateGenres
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.BookRow
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.GenreBookTable
import io.thoth.server.database.tables.GenreSeriesTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.LibraryRow
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.TrackRow
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.addLinks
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.replaceLinks
import io.thoth.server.database.tables.toLibraryRow
import io.thoth.server.database.tables.toTrackRow
import io.thoth.server.database.tables.update
import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import io.thoth.server.file.analyzer.AudioFileAnalyzers
import io.thoth.server.file.scanner.LibraryEntityModel
import io.thoth.server.repositories.AuthorRepository
import io.thoth.server.repositories.BookRepository
import io.thoth.server.repositories.SeriesRepository
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import kotlin.io.path.absolute
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.readAttributes

class TrackManager : KoinComponent {
    private val bookRepository by inject<BookRepository>()
    private val seriesRepository by inject<SeriesRepository>()
    private val authorRepository by inject<AuthorRepository>()
    private val analyzers by inject<AudioFileAnalyzers>()

    private val log = logger {}

    fun needsAnalysis(path: Path): Boolean =
        transaction {
            val known =
                TracksTable
                    .select(TracksTable.fileModifiedAt)
                    .where { TracksTable.path eq path.canonicalString() }
                    .firstOrNull()
                    ?.get(TracksTable.fileModifiedAt)
                    ?: return@transaction true
            known < path.getLastModifiedTime().toMillis()
        }

    fun analyze(
        path: Path,
        library: LibraryEntityModel,
    ): AudioFileAnalysisResult? {
        val attrs =
            try {
                path.readAttributes<BasicFileAttributes>()
            } catch (_: Throwable) {
                // Disappeared between being queued and being picked up, which is normal operation
                return null
            }
        if (!attrs.isRegularFile) return null

        val root = library.folders.firstOrNull { path.startsWith(it) }
        if (root == null) {
            log.error { "'${path.absolute()}' is not under any folder of library '${library.name}'" }
            return null
        }

        return analyzers.forNames(library.fileScanners).analyze(path, attrs, root)
    }

    fun insert(
        scan: AudioFileAnalysisResult,
        libraryId: UUID,
    ) = transaction {
        val library = libraryRow(libraryId)
        val bookId = getOrCreateBook(scan, library)
        val track = TracksTable.selectAll().where { TracksTable.path eq scan.path }.firstOrNull()?.toTrackRow()
        if (track != null) {
            TracksTable.update(
                track.copy(
                    title = scan.title,
                    duration = scan.duration,
                    fileModifiedAt = scan.lastModified,
                    path = scan.path,
                    book = bookId,
                    trackNr = scan.trackNr,
                    scanIndex = library.scanIndex,
                ),
            )
        } else {
            TracksTable.insert(
                TrackRow(
                    id = UUID.randomUUID(),
                    title = scan.title,
                    duration = scan.duration,
                    fileModifiedAt = scan.lastModified,
                    path = scan.path,
                    book = bookId,
                    library = library.id,
                    scanIndex = library.scanIndex,
                    trackNr = scan.trackNr,
                ),
            )
        }
    }

    fun touch(
        paths: List<Path>,
        libraryId: UUID,
    ) = transaction {
        val scanIndex = libraryRow(libraryId).scanIndex
        TracksTable.update({
            (TracksTable.library eq libraryId) and (TracksTable.path inList paths.map { it.canonicalString() })
        }) {
            it[TracksTable.scanIndex] = scanIndex
        }
    }

    // Stamps a whole subtree as seen, for the parts of the tree a scan could not read: not knowing whether a
    // file is still there must not read as knowing that it is gone
    fun touchFolder(
        path: Path,
        libraryId: UUID,
    ) = transaction {
        val scanIndex = libraryRow(libraryId).scanIndex
        val target = path.canonicalString()
        val subtree = LikePattern.ofLiteral(target + File.separator) + "%"
        TracksTable.update({
            ((TracksTable.path eq target) or (TracksTable.path like subtree)) and
                (TracksTable.library eq libraryId)
        }) {
            it[TracksTable.scanIndex] = scanIndex
        }
    }

    fun removeFile(
        path: Path,
        libraryId: UUID,
    ) = transaction {
        TracksTable.deleteWhere {
            (TracksTable.path eq path.canonicalString()) and
                (TracksTable.library eq libraryId)
        }
    }

    fun removeFolder(
        path: Path,
        libraryId: UUID,
    ) = transaction {
        // Rows hold normalised paths, and the separator keeps "/books/Dune" from also matching "/books/Dune 2"
        val target = path.canonicalString()
        val subtree = LikePattern.ofLiteral(target + File.separator) + "%"
        TracksTable.deleteWhere {
            ((TracksTable.path eq target) or (TracksTable.path like subtree)) and
                (TracksTable.library eq libraryId)
        }
    }

    context(_: Transaction)
    private fun libraryRow(libraryId: UUID): LibraryRow =
        LibrariesTable
            .selectAll()
            .where { LibrariesTable.id eq libraryId }
            .single()
            .toLibraryRow()

    context(_: Transaction)
    private fun getOrCreateBook(
        scan: AudioFileAnalysisResult,
        library: LibraryRow,
    ): UUID {
        val authorIds = getOrCreateAuthors(scan, library)
        val book =
            bookRepository.findByName(
                bookTitle = scan.book,
                authorIds = authorIds,
                libraryId = library.id,
            )
        return if (book != null) {
            updateBook(book, scan, authorIds, library)
        } else {
            log.info { "Created new book: ${scan.book}" }
            createBook(scan, authorIds, library)
        }
    }

    context(_: Transaction)
    private fun updateBook(
        book: BookRow,
        scan: AudioFileAnalysisResult,
        authorIds: List<UUID>,
        library: LibraryRow,
    ): UUID {
        val seriesId = scan.series?.let { seriesRepository.getOrCreate(it, library.id, authorIds).id }
        val coverId = book.coverID ?: getOrCreateImage(scan.cover, null)
        val genreIds = getOrCreateGenres(scan.genres)

        BooksTable.update(
            book.copy(
                title = scan.book,
                coverID = coverId,
                language = scan.language,
                description = scan.description,
                narrator = scan.narrator,
            ),
        )
        AuthorBookTable.replaceLinks(AuthorBookTable.book, book.id, AuthorBookTable.authors, authorIds)
        // TODO what to do on a rescan if the user changed things in the UI. Also think about what to do
        //  if one of the tracks does not have a series
        SeriesBookTable.addLinks(SeriesBookTable.book, book.id, SeriesBookTable.series, listOfNotNull(seriesId))
        GenreBookTable.replaceLinks(GenreBookTable.book, book.id, GenreBookTable.genre, genreIds)
        addSeriesGenres(seriesOfBook(book.id), genreIds)
        return book.id
    }

    context(_: Transaction)
    private fun createBook(
        scan: AudioFileAnalysisResult,
        authorIds: List<UUID>,
        library: LibraryRow,
    ): UUID {
        val seriesId = scan.series?.let { seriesRepository.getOrCreate(it, library.id, authorIds).id }
        val coverId = getOrCreateImage(scan.cover, null)
        val genreIds = getOrCreateGenres(scan.genres)

        log.info { "Creating book ${scan.book}" }
        val row =
            BookRow(
                id = UUID.randomUUID(),
                title = scan.book,
                displayTitle = null,
                releaseDate = null,
                publisher = null,
                language = scan.language,
                description = scan.description,
                narrator = scan.narrator,
                isbn = null,
                provider = null,
                providerID = null,
                providerRating = null,
                coverID = coverId,
                library = library.id,
            )
        BooksTable.insert(row)
        AuthorBookTable.replaceLinks(AuthorBookTable.book, row.id, AuthorBookTable.authors, authorIds)
        SeriesBookTable.replaceLinks(SeriesBookTable.book, row.id, SeriesBookTable.series, listOfNotNull(seriesId))
        GenreBookTable.replaceLinks(GenreBookTable.book, row.id, GenreBookTable.genre, genreIds)
        addSeriesGenres(listOfNotNull(seriesId), genreIds)
        return row.id
    }

    // A series has no tags of its own, so it collects the genres of its books
    context(_: Transaction)
    private fun addSeriesGenres(
        seriesIds: List<UUID>,
        genreIds: List<UUID>,
    ) {
        if (genreIds.isEmpty()) return
        seriesIds.forEach { seriesId ->
            GenreSeriesTable.addLinks(GenreSeriesTable.series, seriesId, GenreSeriesTable.genre, genreIds)
        }
    }

    context(_: Transaction)
    private fun seriesOfBook(bookId: UUID): List<UUID> =
        SeriesBookTable
            .select(SeriesBookTable.series)
            .where { SeriesBookTable.book eq bookId }
            .map { it[SeriesBookTable.series].value }

    private fun getOrCreateAuthors(
        scan: AudioFileAnalysisResult,
        library: LibraryRow,
    ): List<UUID> =
        scan.authors.map { author ->
            authorRepository.findByName(author, library.id)?.id ?: run {
                log.info { "Creating author: $author" }
                authorRepository.create(author, library.id).id
            }
        }
}
