package io.thoth.server.file.persister

import io.thoth.database.tables.*
import io.thoth.server.db.access.findByName
import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import kotlinx.coroutines.sync.Semaphore
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import java.nio.file.Path
import kotlin.io.path.absolute

private interface TrackManager {
    suspend fun insertScanResult(scan: AudioFileAnalysisResult, path: Path)
    fun removePath(path: Path)
}

internal class TrackManagerImpl : TrackManager, KoinComponent {
    private val semaphore = Semaphore(1)
    private val log = logger {}

    override suspend fun insertScanResult(scan: AudioFileAnalysisResult, path: Path) {
        this.semaphore.acquire()
        try {
            transaction {
                insertOrUpdateTrack(scan)
            }
        } finally {
            this.semaphore.release()
        }
    }

    private fun insertOrUpdateTrack(scan: AudioFileAnalysisResult): Track {
        val track = Track.getByPath(scan.path)
        return if (track != null) {
            updateTrack(track, scan)
        } else {
            createTrack(scan)
        }
    }

    private fun createTrack(scan: AudioFileAnalysisResult): Track {
        val settings = KeyValueSettings.get()
        val dbBook = getOrCreateBook(scan)
        return Track.new {
            title = scan.title
            duration = scan.duration
            accessTime = scan.lastModified
            path = scan.path
            book = dbBook
            trackNr = scan.trackNr
            scanIndex = settings.scanIndex
        }
    }

    private fun updateTrack(track: Track, scan: AudioFileAnalysisResult): Track {
        val settings = KeyValueSettings.get()
        val dbBook = getOrCreateBook(scan)
        return track.apply {
            title = scan.title
            duration = scan.duration
            accessTime = scan.lastModified
            path = scan.path
            book = dbBook
            trackNr = scan.trackNr
            scanIndex = settings.scanIndex
        }
    }

    override fun removePath(path: Path) = transaction {
        Track.find { TTracks.path like "${path.absolute()}%" }.forEach { it.delete() }
    }

    private fun getOrCreateBook(scan: AudioFileAnalysisResult): Book {
        val author = getOrCreateAuthor(scan)
        val book = Book.getByName(scan.book, author)
        return if (book != null) {
            updateBook(book, scan, author)
        } else {
            log.info("Created new book: ${scan.book}")
            createBook(scan, author)
        }
    }

    private fun updateBook(book: Book, scan: AudioFileAnalysisResult, dbAuthor: Author): Book {
        val dbSeries = if (scan.series != null) getOrCreateSeries(scan, dbAuthor) else null
        val dbImage = if (scan.cover != null) Image.create(scan.cover!!) else null

        return book.apply {
            title = scan.book
            author = dbAuthor
            date = scan.date
            language = scan.language
            description = scan.description
            narrator = scan.narrator
            series = dbSeries
            seriesIndex = scan.seriesIndex
            cover = dbImage
        }
    }

    private fun createBook(scan: AudioFileAnalysisResult, dbAuthor: Author): Book {
        val dbSeries = if (scan.series != null) getOrCreateSeries(scan, dbAuthor) else null
        val dbImage = if (scan.cover != null) Image.create(scan.cover!!) else null
        return Book.new {
            title = scan.book
            author = dbAuthor
            date = scan.date
            language = scan.language
            description = scan.description
            narrator = scan.narrator
            series = dbSeries
            seriesIndex = scan.seriesIndex
            cover = dbImage
        }
    }

    private fun getOrCreateSeries(scan: AudioFileAnalysisResult, dbAuthor: Author): Series {
        val series = Series.findByName(scan.series!!)
        return series?.apply {
            author = dbAuthor
        } ?: run {
            log.info("Created series: ${scan.series}")
            Series.new {
                title = scan.series!!
                author = dbAuthor
            }
        }
    }

    private fun getOrCreateAuthor(scan: AudioFileAnalysisResult) = Author.findByName(scan.author) ?: run {
        log.info("Created author: ${scan.author}")
        Author.new {
            name = scan.author
        }
    }
}
