package io.thoth.server.file.persister

import io.ktor.server.application.*
import io.thoth.common.extensions.get
import io.thoth.database.tables.*
import io.thoth.database.tables.meta.MetaAuthor
import io.thoth.metadata.MetadataProvider
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging.logger
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime
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
    val metaImage = if (authorMetadata.image != null) Image.create(authorMetadata.image!!) else null
    val metaAuthor = MetaAuthor.new {
        provider = authorMetadata.id.provider
        itemID = authorMetadata.id.itemID
        name = authorMetadata.name ?: author.name
        biography = authorMetadata.biography
        imageId = metaImage?.id
    }
    author.linkedTo = metaAuthor
}

private suspend fun ingestBookMetadata(id: UUID) {
    val metadata = get<MetadataProvider>()
    val log = logger {}

    val book = Book.findById(id)!!
    val bookMetadata = metadata.getBookByName(book.title, book.author.name).firstOrNull() ?: run {
        log.warn { "Could not find metadata for book: ${book.title}" }
        return
    }
    log.info { "Inserting metadata for book: ${book.title}" }

    book.apply {
        title = bookMetadata.title ?: title
        date = bookMetadata.date ?: date
        description = bookMetadata.description ?: description
        updateTime = LocalDateTime.now()
        // TODO author = bookMetadata.author
        narrator = bookMetadata.narrator ?: narrator
        // TODO series = bookMetadata
        seriesIndex = bookMetadata.series?.index ?: seriesIndex
        cover = if (bookMetadata.image != null) {
            Image.create(bookMetadata.image!!)
        } else {
            null
        } ?: cover
    }
}

private suspend fun ingestSeriesMetadata(id: UUID) {
    val metadata = get<MetadataProvider>()
    val log = logger {}
    val series = Series.findById(id)!!
    val seriesMetadata = metadata.getSeriesByName(series.title, series.author.name).firstOrNull() ?: run {
        log.warn { "Could not find metadata for series: ${series.title}" }
        return
    }
    log.info { "Inserting metadata for series: ${series.title}" }


    series.apply {
        title = seriesMetadata.name ?: title
        updateTime = LocalDateTime.now()
        description = seriesMetadata.description ?: description
        // TODO author =
    }

}
