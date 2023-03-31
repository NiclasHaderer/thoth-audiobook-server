package io.thoth.server.file

import io.thoth.common.extensions.add
import io.thoth.database.access.create
import io.thoth.database.access.getByPath
import io.thoth.database.tables.Author
import io.thoth.database.tables.Book
import io.thoth.database.tables.Image
import io.thoth.database.tables.Library
import io.thoth.database.tables.Series
import io.thoth.database.tables.TTracks
import io.thoth.database.tables.Track
import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import io.thoth.server.file.analyzer.AudioFileAnalyzerWrapper
import io.thoth.server.services.AuthorRepository
import io.thoth.server.services.BookRepository
import io.thoth.server.services.SeriesRepository
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.io.path.readAttributes
import kotlinx.coroutines.sync.Semaphore
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent

interface TrackManager {
    suspend fun insertScanResult(scan: AudioFileAnalysisResult, path: Path, library: Library)
    suspend fun addPath(path: Path, library: Library)
    fun removePath(path: Path)
}

internal class TrackManagerImpl(
    private val bookRepository: BookRepository,
    private val seriesRepository: SeriesRepository,
    private val authorRepository: AuthorRepository,
    private val analyzer: AudioFileAnalyzerWrapper,
) : TrackManager, KoinComponent {
    private val semaphore = Semaphore(1)
    private val log = logger {}

    override suspend fun addPath(path: Path, library: Library) {
        if (path.isDirectory()) {
            log.warn { "Skipped ${path.absolute()} because it is a directory" }
            return
        }
        val result =
            analyzer.analyze(
                path,
                path.readAttributes(),
                library.folders.map { Path.of(it) }.first { path.contains(it) },
            )
                ?: return log.warn { "Skipped ${path.absolute()} because it contains not enough information" }
        insertScanResult(result, path, library)
    }

    override suspend fun insertScanResult(scan: AudioFileAnalysisResult, path: Path, library: Library) {
        this.semaphore.acquire()
        try {
            transaction { insertOrUpdateTrack(scan, library) }
        } finally {
            this.semaphore.release()
        }
    }

    override fun removePath(path: Path) = transaction {
        Track.find { TTracks.path like "${path.absolute()}%" }.forEach { it.delete() }
    }

    private fun insertOrUpdateTrack(scan: AudioFileAnalysisResult, library: Library): Track {
        val track = Track.getByPath(scan.path)
        return if (track != null) {
            updateTrack(track, scan, library)
        } else {
            createTrack(scan, library)
        }
    }

    private fun createTrack(scan: AudioFileAnalysisResult, libraryModel: Library): Track {
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

    private fun updateTrack(track: Track, scan: AudioFileAnalysisResult, libraryModel: Library): Track {
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

    private fun getOrCreateBook(scan: AudioFileAnalysisResult, libraryModel: Library): Book {
        val author = getOrCreateAuthor(scan, libraryModel)
        val book =
            bookRepository.findByName(
                bookTitle = scan.book,
                authorId = author.id.value,
                libraryId = libraryModel.id.value,
            )
        return if (book != null) {
            updateBook(book, scan, author, libraryModel)
        } else {
            log.info("Created new book: ${scan.book}")
            createBook(scan, author, libraryModel)
        }
    }

    private fun updateBook(book: Book, scan: AudioFileAnalysisResult, dbAuthor: Author, libraryModel: Library): Book {
        val dbSeries = if (scan.series != null) getOrCreateSeries(scan, dbAuthor, libraryModel) else null
        val dbImage = if (scan.cover != null && book.coverID == null) Image.create(scan.cover!!).id else book.coverID

        return book.apply {
            title = scan.book
            coverID = dbImage
            authors = SizedCollection(dbAuthor)
            language = scan.language
            description = scan.description
            narrator = scan.narrator
            series = series.add(dbSeries)
        }
    }

    private fun createBook(scan: AudioFileAnalysisResult, dbAuthor: Author, libraryModel: Library): Book {
        val dbSeries = if (scan.series != null) getOrCreateSeries(scan, dbAuthor, libraryModel) else null
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

    private fun getOrCreateSeries(scan: AudioFileAnalysisResult, dbAuthor: Author, libraryModel: Library): Series {
        val series = seriesRepository.findByName(scan.series!!, libraryModel.id.value)

        return if (series != null) {
            series.authors = series.authors.add(dbAuthor)
            series
        } else {
            log.info("Created series: ${scan.series}")
            Series.new {
                title = scan.series!!
                authors = SizedCollection(dbAuthor)
                library = libraryModel
            }
        }
    }

    private fun getOrCreateAuthor(scan: AudioFileAnalysisResult, libraryModel: Library): Author {
        return authorRepository.findByName(scan.author, libraryModel.id.value)
            ?: run {
                log.info("Created author: ${scan.author}")
                Author.new {
                    name = scan.author
                    library = libraryModel
                }
            }
    }
}
