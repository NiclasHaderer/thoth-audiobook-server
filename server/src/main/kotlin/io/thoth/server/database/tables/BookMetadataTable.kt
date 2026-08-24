package io.thoth.server.database.tables

import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.server.database.extensions.json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDate
import java.util.UUID

sealed class BookMetadata(
    name: String,
) : IdTable<UUID>(name) {
    final override val id = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE)
    final override val primaryKey = PrimaryKey(id)

    val title = text("title").nullable()
    val releaseDate = date("releaseDate").nullable()
    val publisher = varchar("publisher", 255).nullable()
    val language = enumerationByName<MetadataLanguage>("language", 255).nullable()
    val description = text("description").nullable()
    val isbn = varchar("isbn", 255).nullable()
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val providerRating = float("rating").nullable()
    val coverID = reference("cover", ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()

    val genres = json<List<String>>("genres").nullable()
    val narrators = json<List<String>>("narrators").nullable()

    // Claims the relation for this layer. Mostly this is the same as owning a link row, but an empty claim
    // has no row to carry an `addedBy`, so dropping a book's last series would otherwise read as "the user
    // said nothing" and the next scan would put the tagged series back.
    val authorsSet = bool("authorsSet").default(false)
    val seriesSet = bool("seriesSet").default(false)
}

object BookFileMetadataTable : BookMetadata("BookFileMetadata")

object BookAgentMetadataTable : BookMetadata("BookAgentMetadata")

object BookUserMetadataTable : BookMetadata("BookUserMetadata")

data class BookMetadataRow(
    val book: UUID,
    val title: String? = null,
    val releaseDate: LocalDate? = null,
    val publisher: String? = null,
    val language: MetadataLanguage? = null,
    val description: String? = null,
    val isbn: String? = null,
    val provider: String? = null,
    val providerID: String? = null,
    val providerRating: Float? = null,
    val coverID: UUID? = null,
    val genres: List<String>? = null,
    val narrators: List<String>? = null,
    val authorsSet: Boolean = false,
    val seriesSet: Boolean = false,
)

context(_: Transaction)
fun BookMetadata.layer(bookId: UUID): BookMetadataRow =
    selectAll()
        .where { id eq bookId }
        .firstOrNull()
        ?.toBookMetadataRow(this)
        ?: BookMetadataRow(book = bookId)

private fun ResultRow.toBookMetadataRow(table: BookMetadata): BookMetadataRow =
    BookMetadataRow(
        book = this[table.id].value,
        title = this[table.title],
        releaseDate = this[table.releaseDate],
        publisher = this[table.publisher],
        language = this[table.language],
        description = this[table.description],
        isbn = this[table.isbn],
        provider = this[table.provider],
        providerID = this[table.providerID],
        providerRating = this[table.providerRating],
        coverID = this[table.coverID]?.value,
        genres = this[table.genres],
        narrators = this[table.narrators],
        authorsSet = this[table.authorsSet],
        seriesSet = this[table.seriesSet],
    )

context(_: Transaction)
fun BookMetadata.write(row: BookMetadataRow) {
    val updated = update({ id eq row.book }) { write(it, row) }
    if (updated == 0) insert { write(it, row) }
}

private fun BookMetadata.write(
    stmt: UpdateBuilder<*>,
    row: BookMetadataRow,
) {
    stmt[id] = row.book
    stmt[title] = row.title
    stmt[releaseDate] = row.releaseDate
    stmt[publisher] = row.publisher
    stmt[language] = row.language
    stmt[description] = row.description
    stmt[isbn] = row.isbn
    stmt[provider] = row.provider
    stmt[providerID] = row.providerID
    stmt[providerRating] = row.providerRating
    stmt[coverID] = row.coverID
    stmt[genres] = row.genres
    stmt[narrators] = row.narrators
    stmt[authorsSet] = row.authorsSet
    stmt[seriesSet] = row.seriesSet
}
