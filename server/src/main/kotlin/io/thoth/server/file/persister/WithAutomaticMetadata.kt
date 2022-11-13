package io.thoth.server.file.persister

import io.ktor.server.application.*
import io.thoth.common.extensions.classLogger
import io.thoth.common.extensions.get
import io.thoth.database.tables.*
import io.thoth.metadata.MetadataProvider
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime
import java.util.*


@Suppress("UnusedReceiverParameter")
fun Application.withAutomaticMetadata() {
    val tablesWithUpdate = setOf(TBooks, TSeries, TAuthors)
    EntityHook.subscribe {
        if (it.changeType == EntityChangeType.Created) {
            if (tablesWithUpdate.contains(it.entityClass.table)) {
                runBlocking {
                    getMetadataFor(it.entityClass.table, it.entityId.value as UUID)
                }
            }
        }
    }
}


// TODO remove
object A {
    val log = classLogger()
}

private suspend fun getMetadataFor(table: Table, id: UUID) {
    val metadata = get<MetadataProvider>()

    when (table) {
        TBooks -> {
            val book = Book.findById(id)!!
            val bookMetadata = metadata.getBookByName(book.title, book.author.name).firstOrNull() ?: run {
                A.log.warn("Could not find metadata for book: ${book.title}")
                return
            }
            A.log.info("Inserting metadata for book: ${book.title}")

            book.apply {
                title = bookMetadata.title ?: title
                year = bookMetadata.year ?: year
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

        TSeries -> {
            val series = Series.findById(id)!!
            val seriesMetadata = metadata.getSeriesByName(series.title, series.author.name).firstOrNull() ?: run {
                A.log.warn("Could not find metadata for series: ${series.title}")
                return
            }
            A.log.info("Inserting metadata for series: ${series.title}")


            series.apply {
                title = seriesMetadata.name ?: title
                updateTime = LocalDateTime.now()
                description = seriesMetadata.description ?: description
                // TODO author =
            }

        }

        TAuthors -> {
            val author = Author.findById(id)!!
            val authorMetadata = metadata.getAuthorByName(author.name).firstOrNull() ?: run {
                A.log.warn("Could not find metadata for author: ${author.name}")
                return
            }

            A.log.info("Inserting metadata for author: ${author.name}")
            author.apply {
                biography = authorMetadata.biography ?: biography
                image = if (authorMetadata.image != null) {
                    Image.create(authorMetadata.image!!)
                } else {
                    null
                } ?: image
                name = authorMetadata.name ?: name
                updateTime = LocalDateTime.now()
            }
        }
    }
}
