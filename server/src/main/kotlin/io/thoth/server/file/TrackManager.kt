package io.thoth.server.file

import io.thoth.server.common.extensions.lastModifiedInstant
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.server.common.extensions.canonicalString
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.schedules.AutoMatchRequest
import io.thoth.server.schedules.MatchableEntity
import io.thoth.server.schedules.AutoMatcher
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.replaceBookAuthors
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.LibraryRow
import io.thoth.server.database.tables.SeriesFileMetadataTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackRow
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.layer
import io.thoth.server.database.tables.replaceBookSeries
import io.thoth.server.database.tables.toLibraryRow
import io.thoth.server.database.tables.toTrackRow
import io.thoth.server.database.tables.update
import io.thoth.server.database.tables.write
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
    private val autoMatcher by inject<AutoMatcher>()

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
            known < path.lastModifiedInstant()
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

        return analyzers.forNames(library.fileScanners, library.combineFileScannerFields).analyze(path, attrs, root)
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
                    durationMs = scan.durationMs,
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
                    durationMs = scan.durationMs,
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
            bookRepository.findByTaggedName(
                bookTitle = scan.book,
                authorIds = authorIds,
                libraryId = library.id,
            )
        val bookId =
            book?.id ?: run {
                log.info { "Created new book: ${scan.book}" }
                BooksTable.create(library.id).also {
                    autoMatcher.matchOnCommit(AutoMatchRequest(MatchableEntity.BOOK, it, library.id))
                }
            }
        return writeFileLayer(bookId, scan, authorIds, library)
    }

    // Only ever writes the file layer: whatever the user or a metadata agent said about this book lives in
    // its own layer and survives any number of rescans.
    context(_: Transaction)
    private fun writeFileLayer(
        bookId: UUID,
        scan: AudioFileAnalysisResult,
        authorIds: List<UUID>,
        library: LibraryRow,
    ): UUID {
        val seriesId = scan.series?.let { seriesRepository.getOrCreate(it, library.id).id }
        val file = BookFileMetadataTable.layer(bookId)
        val genres = normalizeGenres(scan.genres)

        BookFileMetadataTable.write(
            file.copy(
                title = scan.book,
                coverID = getOrCreateImage(scan.cover, file.coverID),
                language = scan.language,
                description = scan.description,
                narrators = scan.narrators,
                releaseDate = scan.date,
                genres = genres,
            ),
        )
        // The whole file layer is what the last imported track's tags say, relations included. Tracks of one
        // book that disagree about their series or authors leave it up to whichever is imported last, the same
        // way they already do for the title or the narrator.
        replaceBookAuthors(bookId, MetadataLayer.FILE, authorIds)
        replaceBookSeries(bookId, MetadataLayer.FILE, listOfNotNull(seriesId).associateWith { scan.seriesIndex })
        // Unset deletion marker, since the book has a track again
        BooksTable.update({ BooksTable.id eq bookId }) { it[deferDeletionUntil] = null }
        AuthorTable.update({ AuthorTable.id inList authorIds }) { it[deferDeletionUntil] = null }
        SeriesTable.update({ SeriesTable.id inList listOfNotNull(seriesId) }) { it[deferDeletionUntil] = null }
        return bookId
    }

    // The same genre spelled differently by two files is one genre
    private fun normalizeGenres(names: List<String>): List<String> = names.distinctBy { it.lowercase() }

    private fun getOrCreateAuthors(
        scan: AudioFileAnalysisResult,
        library: LibraryRow,
    ): List<UUID> =
        scan.authors.map { author ->
            authorRepository.findByTaggedName(author, library.id)?.id ?: run {
                log.info { "Creating author: $author" }
                authorRepository.create(author, library.id).id
            }
        }
}
