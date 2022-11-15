package io.thoth.server.file.persister

import io.ktor.server.application.*
import io.thoth.common.extensions.get
import io.thoth.database.tables.*
import io.thoth.database.tables.meta.MetaAuthor
import io.thoth.database.tables.meta.MetaBook
import io.thoth.database.tables.meta.MetaSeries
import io.thoth.metadata.MetadataProvider
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging.logger
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.sql.Table
import java.util.*


@Suppress("UnusedReceiverParameter")
fun Application.withAutomaticMetadata() {
    val tablesWithUpdate = setOf(TBooks, TSeries, TAuthors)
    EntityHook.subscribe {
        if (it.changeType != EntityChangeType.Created) return@subscribe
        if (!tablesWithUpdate.contains(it.entityClass.table)) return@subscribe
        runBlocking {
            getMetadataFor(it.entityClass.table, it.entityId.value as UUID)
        }
    }
}


private suspend fun getMetadataFor(table: Table, id: UUID) {
    when (table) {
        TBooks -> ingestBookMetadata(id)
        TSeries -> ingestSeriesMetadata(id)
        TAuthors -> ingestAuthorMetadata(id)
    }
}

private suspend fun ingestAuthorMetadata(id: UUID) {
    val metadata = get<MetadataProvider>()
    val log = logger {}
    val author = Author.findById(id)!!
    val authorMetadata = metadata.getAuthorByName(author.name).firstOrNull()
        ?: return log.warn { "Could not find metadata for author: ${author.name}" }

    log.info { "Inserting metadata for author: ${author.name}" }
    val metaImage = if (authorMetadata.image != null) Image.create(authorMetadata.image!!).id else null
    val metaAuthor = MetaAuthor.new {
        name = authorMetadata.name ?: author.name
        provider = authorMetadata.id.provider
        itemID = authorMetadata.id.itemID
        biography = authorMetadata.biography
        imageId = metaImage
        // TODO add new attributes
        //  website = authorMetadata.website
        //  born = authorMetadata.born
        //  born = authorMetadata.born
        //  died = authorMetadata.died
        //  books = authorMetadata.books
        //  series = authorMetadata.series
        //  genres = authorMetadata.genres

    }
    author.linkedTo = metaAuthor
}

private suspend fun ingestBookMetadata(id: UUID) {
    val metadata = get<MetadataProvider>()
    val log = logger {}

    val book = Book.findById(id)!!
    val bookMetadata = metadata.getBookByName(book.title, book.author.name).firstOrNull()
        ?: return log.warn { "Could not find metadata for book: ${book.title}" }

    log.info { "Inserting metadata for book: ${book.title}" }
    val metaImage = if (bookMetadata.image != null) Image.create(bookMetadata.image!!).id else null
    MetaBook.new {
        title = bookMetadata.title ?: title
        provider = bookMetadata.id.provider
        itemID = bookMetadata.id.itemID
        date = bookMetadata.date
        description = bookMetadata.description
        narrator = bookMetadata.narrator
        cover = metaImage
        // TODO add new attributes
        //  language = bookMetadata.language
        //  rating = bookMetadata.rating
        //  authors = bookMetadata.author
        //  series = bookMetadata.series
        //  genres = bookMetadata.genres
    }
}

private suspend fun ingestSeriesMetadata(id: UUID) {
    val metadata = get<MetadataProvider>()
    val log = logger {}
    val series = Series.findById(id)!!
    val seriesMetadata = metadata.getSeriesByName(series.title, series.author.name).firstOrNull()
        ?: return log.warn { "Could not find metadata for series: ${series.title}" }

    log.info { "Inserting metadata for series: ${series.title}" }
    val metaImage = if (seriesMetadata.image != null) Image.create(seriesMetadata.image!!).id else null

    MetaSeries.new {
        title = seriesMetadata.name ?: title
        provider = seriesMetadata.id.provider
        itemID = seriesMetadata.id.itemID
        totalBooks = seriesMetadata.amount
        cover = metaImage
        description = seriesMetadata.description
        // TODO add new attributes
        //  primaryWorks = seriesMetadata.primaryWorks
        //  authors = seriesMetadata.authors
        //  books = seriesMetadata.books
        //  genres = seriesMetadata.genres
    }

}
