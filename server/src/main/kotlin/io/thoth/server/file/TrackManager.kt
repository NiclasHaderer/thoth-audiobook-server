package io.thoth.server.file

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.server.common.extensions.add
import io.thoth.server.common.extensions.findOne
import io.thoth.server.common.extensions.canonicalString
import io.thoth.server.database.access.create
import io.thoth.server.database.access.getOrCreate
import io.thoth.server.database.access.hasBeenUpdated
import io.thoth.server.database.access.markAsTouched
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.GenreEntity
import io.thoth.server.database.tables.ImageEntity
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.database.tables.TracksTable
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
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.util.UUID
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
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
            val track =
                TrackEntity.findOne { TracksTable.path eq path.canonicalString() } ?: return@transaction true
            track.hasBeenUpdated(path.getLastModifiedTime().toMillis())
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
        val library = LibraryEntity[libraryId]
        val track = TrackEntity.findOne { TracksTable.path eq scan.path }
        if (track != null) {
            updateTrack(track, scan, library).also { track.markAsTouched() }
        } else {
            createTrack(scan, library)
        }
    }

    fun touch(
        paths: List<Path>,
        libraryId: UUID,
    ) = transaction {
        val scanIndex = LibraryEntity[libraryId].scanIndex
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
        val scanIndex = LibraryEntity[libraryId].scanIndex
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

    private fun createTrack(
        scan: AudioFileAnalysisResult,
        libraryModel: LibraryEntity,
    ): TrackEntity {
        val dbBook = getOrCreateBook(scan, libraryModel)
        return TrackEntity.new {
            title = scan.title
            duration = scan.duration
            accessTime = scan.lastModified
            path = scan.path
            book = dbBook
            trackNr = scan.trackNr
            scanIndex = libraryModel.scanIndex
            updateTime = LocalDateTime.now()
            library = libraryModel
        }
    }

    private fun updateTrack(
        track: TrackEntity,
        scan: AudioFileAnalysisResult,
        libraryModel: LibraryEntity,
    ): TrackEntity {
        val dbBook = getOrCreateBook(scan, libraryModel)
        return track.apply {
            // Only when the file itself moved on. Stamping every pass would dirty the row on every rescan, and
            // Exposed turns that into an UPDATE per track where an unchanged file used to cost nothing.
            if (accessTime != scan.lastModified) updateTime = LocalDateTime.now()
            title = scan.title
            duration = scan.duration
            accessTime = scan.lastModified
            path = scan.path
            book = dbBook
            trackNr = scan.trackNr
            scanIndex = libraryModel.scanIndex
        }
    }

    private fun getOrCreateBook(
        scan: AudioFileAnalysisResult,
        libraryModel: LibraryEntity,
    ): BookEntity {
        val authors = getOrCreateAuthors(scan, libraryModel)
        val book =
            bookRepository.findByName(
                bookTitle = scan.book,
                authorIds = authors.map { it.id.value },
                libraryId = libraryModel.id.value,
            )
        return if (book != null) {
            updateBook(book, scan, authors, libraryModel)
        } else {
            log.info { "Created new book: ${scan.book}" }
            createBook(scan, authors, libraryModel)
        }
    }

    private fun updateBook(
        book: BookEntity,
        scan: AudioFileAnalysisResult,
        dbAuthors: List<AuthorEntity>,
        libraryModel: LibraryEntity,
    ): BookEntity {
        val dbSeries =
            if (scan.series != null) {
                seriesRepository.getOrCreate(scan.series!!, libraryModel.id.value, dbAuthors)
            } else {
                null
            }
        val dbImage =
            if (scan.cover != null && book.coverID == null) {
                ImageEntity.create(scan.cover!!).id
            } else {
                book.coverID
            }

        val dbGenres = GenreEntity.getOrCreate(scan.genres)

        return book
            .apply {
                title = scan.book
                coverID = dbImage
                authors = SizedCollection(dbAuthors)
                language = scan.language
                description = scan.description
                narrator = scan.narrator
                series = series.add(dbSeries)
                setGenres(dbGenres)
            }.also { addSeriesGenres(it.series, dbGenres) }
    }

    // Rewriting an unchanged relation costs a DELETE plus an INSERT, and every track of a book comes through here
    private fun BookEntity.setGenres(wanted: List<GenreEntity>) {
        if (genres.map { it.id }.toSet() != wanted.map { it.id }.toSet()) genres = SizedCollection(wanted)
    }

    // A series has no tags of its own, so it collects the genres of its books
    private fun addSeriesGenres(
        series: Iterable<SeriesEntity>,
        genres: List<GenreEntity>,
    ) {
        if (genres.isEmpty()) return
        series.forEach { entry ->
            val known = entry.genres.map { it.id }.toSet()
            val missing = genres.filterNot { it.id in known }
            if (missing.isNotEmpty()) entry.genres = SizedCollection(entry.genres + missing)
        }
    }

    private fun createBook(
        scan: AudioFileAnalysisResult,
        dbAuthor: List<AuthorEntity>,
        libraryModel: LibraryEntity,
    ): BookEntity {
        val dbSeries =
            if (scan.series != null) {
                seriesRepository.getOrCreate(scan.series!!, libraryModel.id.value, dbAuthor)
            } else {
                null
            }
        val dbImage = if (scan.cover != null) ImageEntity.create(scan.cover!!) else null
        val dbSeriesList = if (dbSeries != null) listOf(dbSeries) else listOf()
        val dbGenres = GenreEntity.getOrCreate(scan.genres)

        log.info { "Creating book ${scan.book}" }
        return BookEntity
            .new {
                title = scan.book
                authors = SizedCollection(dbAuthor)
                language = scan.language
                description = scan.description
                narrator = scan.narrator
                series = SizedCollection(dbSeriesList)
                coverID = dbImage?.id
                library = libraryModel
                if (dbGenres.isNotEmpty()) genres = SizedCollection(dbGenres)
            }.also { addSeriesGenres(dbSeriesList, dbGenres) }
    }

    private fun getOrCreateAuthors(
        scan: AudioFileAnalysisResult,
        libraryModel: LibraryEntity,
    ): List<AuthorEntity> =
        scan.authors.map { author ->
            authorRepository.findByName(author, libraryModel.id.value) ?: run {
                log.info { "Creating author: $author" }
                AuthorEntity
                    .new {
                        name = author
                        library = libraryModel
                    }.also { it.flush() }
            }
        }
}
