package io.github.huiibuh.file.persister

import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Image
import io.github.huiibuh.db.tables.KeyValueSettings
import io.github.huiibuh.db.tables.ProviderID
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.extensions.classLogger
import io.github.huiibuh.file.analyzer.AudioFileAnalysisResult
import io.github.huiibuh.metadata.MetadataProvider
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Path
import kotlin.io.path.absolute

private interface TrackManager {
    suspend fun insertScanResult(scan: AudioFileAnalysisResult, path: Path)
    fun removePath(path: Path)
}

internal class TrackManagerImpl : TrackManager, KoinComponent {
    private val metadataProvider by inject<MetadataProvider>()
    private val log = classLogger()

    override suspend fun insertScanResult(scan: AudioFileAnalysisResult, path: Path) {
        getOrCreateBook(scan)
        getOrCreateTrack(scan)
    }

    private suspend fun getOrCreateTrack(scan: AudioFileAnalysisResult): Track {
        val track = Track.getByPath(scan.path)
        return if (track != null) {
            updateTrack(track, scan)
        } else {
            createTrack(scan)
        }
    }

    private suspend fun createTrack(scan: AudioFileAnalysisResult): Track {
        val settings = KeyValueSettings.get()
        val dbBook = getOrCreateBook(scan)
        return transaction {
            Track.new {
                title = scan.title
                duration = scan.duration
                accessTime = scan.lastModified
                path = scan.path
                book = dbBook
                trackNr = scan.trackNr
                scanIndex = settings.scanIndex
            }
        }
    }

    private suspend fun updateTrack(track: Track, scan: AudioFileAnalysisResult): Track {
        val settings = KeyValueSettings.get()
        val dbBook = getOrCreateBook(scan)
        return transaction {
            track.apply {
                title = scan.title
                duration = scan.duration
                accessTime = scan.lastModified
                path = scan.path
                book = dbBook
                trackNr = scan.trackNr
                scanIndex = settings.scanIndex
            }
        }
    }

    override fun removePath(path: Path) = transaction {
        Track.find { TTracks.path like "${path.absolute()}%" }
            .forEach { it.delete() }
    }

    private suspend fun getOrCreateBook(scan: AudioFileAnalysisResult): Book {
        val author = getOrCreateAuthor(scan)
        val book = Book.getByName(scan.book, author)
        return if (book != null) {
            updateBook(book, scan)
        } else {
            createBook(scan)
        }
    }

    private suspend fun updateBook(book: Book, scan: AudioFileAnalysisResult): Book {
        val dbAuthor = getOrCreateAuthor(scan)
        val dbSeries = if (scan.series != null) getOrCreateSeries(scan) else null
        val dbImage = if (scan.cover != null) Image.create(scan.cover!!) else null

        return transaction {
            book.apply {
                title = scan.book
                author = dbAuthor
                year = scan.year
                language = scan.language
                description = scan.description
                narrator = scan.narrator
                series = dbSeries
                seriesIndex = scan.seriesIndex
                cover = dbImage
            }
        }
    }

    private suspend fun createBook(scan: AudioFileAnalysisResult): Book {
        val dbAuthor = getOrCreateAuthor(scan)
        val dbSeries = if (scan.series != null) getOrCreateSeries(scan) else null
        var dbImage = if (scan.cover != null) Image.create(scan.cover!!) else null

        val response = metadataProvider.getBookByName(scan.book).firstOrNull() ?: return transaction {
            Book.new {
                title = scan.book
                author = dbAuthor
                year = scan.year
                language = scan.language
                description = scan.description
                narrator = scan.narrator
                series = dbSeries
                seriesIndex = scan.seriesIndex
                cover = dbImage
            }
        }

        if (dbImage == null && response.image != null) {
            dbImage = Image.create(response.image!!)
        }

        val book = transaction {
            Book.new {
                title = scan.book
                author = dbAuthor
                year = scan.year
                language = scan.language
                description = scan.description ?: response.description
                narrator = scan.narrator ?: response.narrator
                series = dbSeries
                seriesIndex = scan.seriesIndex ?: response.series?.index
                cover = dbImage
            }
        }
        log.info("Create new book ${book.title}")
        return book
    }

    private suspend fun getOrCreateSeries(scan: AudioFileAnalysisResult): Series {
        val series = Series.getByName(scan.series!!)
        return if (series != null) {
            updateSeries(series, scan)
        } else {
            createSeries(scan)
        }
    }

    private suspend fun updateSeries(series: Series, scan: AudioFileAnalysisResult): Series {
        val dbAuthor = getOrCreateAuthor(scan)
        return transaction {
            series.apply {
                title = scan.series!!
                author = dbAuthor
            }
        }
    }

    private suspend fun createSeries(scan: AudioFileAnalysisResult): Series {
        val dbAuthor = getOrCreateAuthor(scan)
        val response = metadataProvider.getSeriesByName(scan.series!!).firstOrNull() ?: return transaction {
            Series.new {
                title = scan.series!!
                author = dbAuthor
            }
        }

        val series = transaction {
            Series.new {
                title = scan.series!!
                author = dbAuthor
                description = response.description
                providerID = ProviderID.new {
                    provider = response.id.provider
                    itemID = response.id.itemID
                }
            }
        }
        log.info("Created new series ${series.title}")
        return series
    }

    private suspend fun getOrCreateAuthor(scan: AudioFileAnalysisResult): Author {
        val author = Author.getByName(scan.author)
        return if (author != null) {
            updateAuthor(author, scan)
        } else {
            createAuthor(scan)
        }
    }

    private fun updateAuthor(author: Author, scan: AudioFileAnalysisResult) = transaction {
        author.apply {
            name = scan.author
        }
    }

    private suspend fun createAuthor(scan: AudioFileAnalysisResult): Author {
        val response = metadataProvider.getAuthorByName(scan.author).firstOrNull() ?: return transaction {
            Author.new {
                name = scan.author
            }
        }

        val dbImage = if (response.image != null) Image.create(response.image!!) else null

        val author = transaction {
            Author.new {
                name = scan.author
                biography = response.biography
                image = dbImage
                providerID = ProviderID.new {
                    provider = response.id.provider
                    itemID = response.id.itemID
                }
            }
        }
        log.info("Create new author ${author.name}")
        return author
    }
}
