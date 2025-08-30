package io.thoth.server.file

import io.thoth.server.common.extensions.add
import io.thoth.server.common.extensions.findOne
import io.thoth.server.database.access.create
import io.thoth.server.database.access.markAsTouched
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.ImageEntity
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import io.thoth.server.file.analyzer.AudioFileAnalyzers
import io.thoth.server.repositories.AuthorRepository
import io.thoth.server.repositories.BookRepository
import io.thoth.server.repositories.SeriesRepository
import mu.KotlinLogging.logger
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.like
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.isRegularFile
import kotlin.io.path.readAttributes

object TrackManager : KoinComponent {
    private val bookRepository by inject<BookRepository>()
    private val seriesRepository by inject<SeriesRepository>()
    private val authorRepository by inject<AuthorRepository>()
    private val analyzers by inject<AudioFileAnalyzers>()

    private val log = logger {}

    fun addPath(
        path: Path,
        library: LibraryEntity,
    ) {
        transaction {
            require(path.isRegularFile()) { "Cannot add folder to library" }
            val libPath = library.folders.map { Path.of(it) }.first { path.startsWith(it) }
            val analyzer = analyzers.forLibrary(library)
            val result =
                analyzer.analyze(path, path.readAttributes(), libPath)
                    ?: return@transaction log.warn {
                        "Could not extract al necessary information for '${path.absolute()}'"
                    }
            insertScanResult(result, library)
        }
    }

    fun removeFolder(
        path: Path,
        library: LibraryEntity,
    ) {
        transaction {
            TracksTable.deleteWhere {
                TracksTable.path like "${path.absolute()}%" and (TracksTable.library eq library.id)
            }
        }
    }

    private fun insertScanResult(
        scan: AudioFileAnalysisResult,
        library: LibraryEntity,
    ) {
        val track = TrackEntity.findOne { TracksTable.path like scan.path }
        if (track != null) {
            updateTrack(track, scan, library).also { track.markAsTouched() }
        } else {
            createTrack(scan, library)
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
            log.info("Created new book: ${scan.book}")
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

        return book.apply {
            title = scan.book
            coverID = dbImage
            authors = SizedCollection(dbAuthors)
            language = scan.language
            description = scan.description
            narrator = scan.narrator
            series = series.add(dbSeries)
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

        log.info("Creating book ${scan.book}")
        return BookEntity.new {
            title = scan.book
            authors = SizedCollection(dbAuthor)
            language = scan.language
            description = scan.description
            narrator = scan.narrator
            series = SizedCollection(dbSeriesList)
            coverID = dbImage?.id
            library = libraryModel
        }
    }

    private fun getOrCreateAuthors(
        scan: AudioFileAnalysisResult,
        libraryModel: LibraryEntity,
    ): List<AuthorEntity> =
        scan.authors.map { author ->
            authorRepository.findByName(author, libraryModel.id.value) ?: run {
                log.info("Creating author: $author")
                AuthorEntity
                    .new {
                        name = author
                        library = libraryModel
                    }
            }
        }
}
