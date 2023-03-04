package io.thoth.server.file.persister

import io.thoth.common.extensions.add
import io.thoth.database.access.create
import io.thoth.database.access.findByName
import io.thoth.database.access.getByPath
import io.thoth.database.tables.Author
import io.thoth.database.tables.Book
import io.thoth.database.tables.Image
import io.thoth.database.tables.Series
import io.thoth.database.tables.TTracks
import io.thoth.database.tables.Track
import io.thoth.models.LibraryModel
import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import io.thoth.server.file.analyzer.AudioFileAnalyzerWrapper
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.io.path.readAttributes
import kotlinx.coroutines.sync.Semaphore
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface TrackManager {
    suspend fun insertScanResult(scan: AudioFileAnalysisResult, path: Path, library: LibraryModel)
    suspend fun addPath(path: Path, library: LibraryModel)
    fun removePath(path: Path)
}

internal class TrackManagerImpl : TrackManager, KoinComponent {
    private val analyzer by inject<AudioFileAnalyzerWrapper>()
    private val semaphore = Semaphore(1)
    private val log = logger {}

    override suspend fun addPath(path: Path, library: LibraryModel) {
        if (path.isDirectory()) {
            log.warn { "Skipped ${path.absolute()} because it is a directory" }
            return
        }
        val result =
            analyzer.analyze(path, path.readAttributes())
                ?: return log.warn { "Skipped ${path.absolute()} because it contains not enough information" }
        insertScanResult(result, path, library)
    }

    override suspend fun insertScanResult(scan: AudioFileAnalysisResult, path: Path, library: LibraryModel) {
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

    private fun insertOrUpdateTrack(scan: AudioFileAnalysisResult, library: LibraryModel): Track {
        val track = Track.getByPath(scan.path)
        return if (track != null) {
            updateTrack(track, scan, library)
        } else {
            createTrack(scan, library)
        }
    }

    private fun createTrack(scan: AudioFileAnalysisResult, library: LibraryModel): Track {
        val dbBook = getOrCreateBook(scan)
        return Track.new {
            title = scan.title
            duration = scan.duration
            accessTime = scan.lastModified
            path = scan.path
            book = dbBook
            trackNr = scan.trackNr
            scanIndex = library.scanIndex
        }
    }

    private fun updateTrack(track: Track, scan: AudioFileAnalysisResult, library: LibraryModel): Track {
        val dbBook = getOrCreateBook(scan)
        return track.apply {
            title = scan.title
            duration = scan.duration
            accessTime = scan.lastModified
            path = scan.path
            book = dbBook
            trackNr = scan.trackNr
            scanIndex = library.scanIndex
        }
    }

    private fun getOrCreateBook(scan: AudioFileAnalysisResult): Book {
        val author = getOrCreateAuthor(scan)
        val book = Book.findByName(scan.book, author)
        return if (book != null) {
            updateBook(book, scan, author)
        } else {
            log.info("Created new book: ${scan.book}")
            createBook(scan, author)
        }
    }

    private fun updateBook(book: Book, scan: AudioFileAnalysisResult, dbAuthor: Author): Book {
        val dbSeries = if (scan.series != null) getOrCreateSeries(scan, dbAuthor) else null
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

    private fun createBook(scan: AudioFileAnalysisResult, dbAuthor: Author): Book {
        val dbSeries = if (scan.series != null) getOrCreateSeries(scan, dbAuthor) else null
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
        }
    }

    private fun getOrCreateSeries(scan: AudioFileAnalysisResult, dbAuthor: Author): Series {
        val series = Series.findByName(scan.series!!)

        return if (series != null) {
            series.authors = series.authors.add(dbAuthor)
            series
        } else {
            log.info("Created series: ${scan.series}")
            Series.new {
                title = scan.series!!
                authors = SizedCollection(dbAuthor)
            }
        }
    }

    private fun getOrCreateAuthor(scan: AudioFileAnalysisResult): Author {
        return Author.findByName(scan.author)
            ?: run {
                log.info("Created author: ${scan.author}")
                Author.new { name = scan.author }
            }
    }
}
