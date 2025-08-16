package io.thoth.server.file

import io.thoth.server.common.extensions.add
import io.thoth.server.database.access.create
import io.thoth.server.database.access.getByPath
import io.thoth.server.database.access.markAsTouched
import io.thoth.server.database.tables.*
import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import io.thoth.server.file.analyzer.AudioFileAnalyzers
import io.thoth.server.file.analyzer.impl.AudioFileAnalyzerWrapper
import io.thoth.server.repositories.AuthorRepository
import io.thoth.server.repositories.BookRepository
import io.thoth.server.repositories.SeriesRepository
import kotlinx.coroutines.sync.Semaphore
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.io.path.readAttributes

interface TrackManager {
    suspend fun insertScanResult(
        scan: AudioFileAnalysisResult,
        path: Path,
        library: Library,
    )

    suspend fun addPath(
        path: Path,
        library: Library,
    )

    fun removePath(path: Path)
}

class TrackManagerImpl :
    TrackManager,
    KoinComponent {
    private val bookRepository by inject<BookRepository>()
    private val seriesRepository by inject<SeriesRepository>()
    private val authorRepository by inject<AuthorRepository>()
    private val analyzers by inject<AudioFileAnalyzers>()

    private val semaphore = Semaphore(1)
    private val log = logger {}

    override suspend fun addPath(
        path: Path,
        library: Library,
    ) {
        if (path.isDirectory()) {
            log.warn { "Skipped ${path.absolute()} because it is a directory" }
            return
        }
        val libPath = library.folders.map { Path.of(it) }.first { path.startsWith(it) }
        val libAnalyzer =
            analyzers.filter { analyzer -> analyzer.name in library.fileScanners.map { libScanner -> libScanner.name } }

        if (libAnalyzer.isEmpty()) {
            return log.warn {
                "Skipped ${path.absolute()} because it is not supported by any scanner"
                " (available scanners: ${analyzers.map { it.name }})"
                " (library scanners: ${library.fileScanners.map { it.name }})"
            }
        }

        val analyzer = AudioFileAnalyzerWrapper(libAnalyzer)
        val result =
            analyzer.analyze(path, path.readAttributes(), libPath)
                ?: return log.warn { "Skipped ${path.absolute()} because it contains not enough information" }
        insertScanResult(result, path, library)
    }

    override suspend fun insertScanResult(
        scan: AudioFileAnalysisResult,
        path: Path,
        library: Library,
    ) {
        this.semaphore.acquire()
        try {
            transaction { insertOrUpdateTrack(scan, library) }
        } finally {
            this.semaphore.release()
        }
    }

    override fun removePath(path: Path) =
        transaction {
            Track.find { TTracks.path like "${path.absolute()}%" }.forEach { it.delete() }
        }

    private fun insertOrUpdateTrack(
        scan: AudioFileAnalysisResult,
        library: Library,
    ): Track {
        val track = Track.getByPath(scan.path)
        return if (track != null) {
            updateTrack(track, scan, library).also { track.markAsTouched() }
        } else {
            createTrack(scan, library)
        }
    }

    private fun createTrack(
        scan: AudioFileAnalysisResult,
        libraryModel: Library,
    ): Track {
        val dbBook = getOrCreateBook(scan, libraryModel)
        return Track.new {
            title = scan.title
            duration = scan.duration
            accessTime = scan.lastModified
            path = scan.path
            book = dbBook
            trackNr = scan.trackNr
            scanIndex = libraryModel.scanIndex
        }
    }

    private fun updateTrack(
        track: Track,
        scan: AudioFileAnalysisResult,
        libraryModel: Library,
    ): Track {
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
        libraryModel: Library,
    ): Book {
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
        book: Book,
        scan: AudioFileAnalysisResult,
        dbAuthors: List<Author>,
        libraryModel: Library,
    ): Book {
        val dbSeries =
            if (scan.series != null) {
                seriesRepository.getOrCreate(scan.series!!, libraryModel.id.value, dbAuthors)
            } else {
                null
            }
        val dbImage = if (scan.cover != null && book.coverID == null) Image.create(scan.cover!!).id else book.coverID

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
        dbAuthor: List<Author>,
        libraryModel: Library,
    ): Book {
        val dbSeries =
            if (scan.series != null) {
                seriesRepository.getOrCreate(scan.series!!, libraryModel.id.value, dbAuthor)
            } else {
                null
            }
        val dbImage = if (scan.cover != null) Image.create(scan.cover!!) else null
        val dbSeriesList = if (dbSeries != null) listOf(dbSeries) else listOf()

        return Book.new {
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
        libraryModel: Library,
    ): List<Author> =
        scan.authors.map { author ->
            authorRepository.findByName(author, libraryModel.id.value)
                ?: run {
                    log.info("Created author: ${scan.authors}")
                    Author.new {
                        name = author
                        library = libraryModel
                    }
                }
        }
}
