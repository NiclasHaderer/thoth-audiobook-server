package io.thoth.server.database.views

import io.thoth.server.common.exposed.View
import io.thoth.server.common.exposed.layered
import io.thoth.server.database.extensions.json
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.BookAgentMetadataTable
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.BookMetadata
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.SeriesBookTable
import org.jetbrains.exposed.v1.core.Case
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.select

object BookMetadataView : View("BookMetadata") {
    val id = javaUUID("id")
    val library = javaUUID("library")
    val title = text("title")
    val releaseDate = date("releaseDate").nullable()
    val publisher = varchar("publisher", 255).nullable()
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val narrator = varchar("narrator", 255).nullable()
    val isbn = varchar("isbn", 255).nullable()
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val providerRating = float("rating").nullable()
    val cover = javaUUID("cover").nullable()
    val genres = json<List<String>>("genres").nullable()

    val authorsFrom = enumerationByName<MetadataLayer>("authorsFrom", 8)
    val seriesFrom = enumerationByName<MetadataLayer>("seriesFrom", 8)

    override fun body() =
        BooksTable
            .join(LibrariesTable, JoinType.INNER, BooksTable.library, LibrariesTable.id)
            .join(BookFileMetadataTable, JoinType.LEFT, BooksTable.id, BookFileMetadataTable.id)
            .join(BookAgentMetadataTable, JoinType.LEFT, BooksTable.id, BookAgentMetadataTable.id)
            .join(BookUserMetadataTable, JoinType.LEFT, BooksTable.id, BookUserMetadataTable.id)
            .select(
                BooksTable.id,
                BooksTable.library,
                resolve { title },
                resolve { releaseDate },
                resolve { publisher },
                resolve { language },
                resolve { description },
                resolve { narrator },
                resolve { isbn },
                resolve { provider },
                resolve { providerID },
                resolve { providerRating },
                resolve { coverID },
                resolve { genres },
                layerOf({ authorsSet }, fileNames(AuthorBookTable.book, AuthorBookTable.addedBy), "authorsFrom"),
                layerOf({ seriesSet }, fileNames(SeriesBookTable.book, SeriesBookTable.addedBy), "seriesFrom"),
            )

    private fun <T> resolve(pick: BookMetadata.() -> Column<T>) =
        layered(
            user = BookUserMetadataTable,
            agent = BookAgentMetadataTable,
            file = BookFileMetadataTable,
            preferFile = LibrariesTable.preferEmbeddedMetadata,
            pick = pick,
        )

    /**
     * A user override always wins. The file tags only come first when the library prefers them *and* they
     * actually name something
     */
    private fun layerOf(
        flag: BookMetadata.() -> Column<Boolean>,
        fileNamesSomething: Op<Boolean>,
        alias: String,
    ) = Case()
        .When(BookUserMetadataTable.flag(), stringLiteral(MetadataLayer.USER.name))
        .When(LibrariesTable.preferEmbeddedMetadata and fileNamesSomething, stringLiteral(MetadataLayer.FILE.name))
        .When(BookAgentMetadataTable.flag(), stringLiteral(MetadataLayer.AGENT.name))
        .Else(stringLiteral(MetadataLayer.FILE.name))
        .alias(alias)

    private fun fileNames(
        bookColumn: Column<*>,
        addedBy: Column<MetadataLayer>,
    ): Op<Boolean> =
        exists(
            bookColumn.table
                .select(bookColumn)
                .where { (bookColumn eq BooksTable.id) and (addedBy eq MetadataLayer.FILE) },
        )
}
