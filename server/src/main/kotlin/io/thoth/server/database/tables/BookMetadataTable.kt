package io.thoth.server.database.tables

import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.models.ChapterMark
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

enum class BookField : LayerField {
    TITLE,
    AUTHORS,
    SERIES,
    PROVIDER,
    PROVIDER_ID,
    PROVIDER_RATING,
    RELEASE_DATE,
    PUBLISHER,
    LANGUAGE,
    DESCRIPTION,
    NARRATORS,
    GENRES,
    ISBN,
    COVER_ID,
    CHAPTERS,
}

sealed class BookMetadata(
    name: String,
) : IdTable<UUID>(name) {
    final override val id = reference("book_id", BookTable, onDelete = ReferenceOption.CASCADE)
    final override val primaryKey = PrimaryKey(id)

    val title = text(BookField.TITLE.column).nullable()
    val releaseDate = date(BookField.RELEASE_DATE.column).nullable()
    val publisher = varchar(BookField.PUBLISHER.column, 255).nullable()
    val language = enumerationByName<MetadataLanguage>(BookField.LANGUAGE.column, 255).nullable()
    val description = text(BookField.DESCRIPTION.column).nullable()
    val isbn = varchar(BookField.ISBN.column, 255).nullable()
    val provider = varchar(BookField.PROVIDER.column, 255).nullable()
    val providerId = varchar(BookField.PROVIDER_ID.column, 255).nullable()
    val providerRating = float(BookField.PROVIDER_RATING.column).nullable()
    val coverId = reference(BookField.COVER_ID.column, ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()

    val genres = json<List<String>>(BookField.GENRES.column).nullable()
    val narrators = json<List<String>>(BookField.NARRATORS.column).nullable()

    // Never written by the file layer: the chapters the files name depend on all tracks of a book, so they are
    // derived from the tracks
    val chapters = json<List<ChapterMark>>(BookField.CHAPTERS.column).nullable()

    // For the relations a claim matters even with links: an empty claim has no link row to carry an `addedBy`,
    // so dropping a book's last series would otherwise read as "the user said nothing" and the next scan would
    // put the tagged series back.
    val claimed = json<Set<BookField>>("claimed").default(emptySet())
}

object BookFileMetadataTable : BookMetadata("book_file_metadata")

object BookAgentMetadataTable : BookMetadata("book_agent_metadata")

object BookUserMetadataTable : BookMetadata("book_user_metadata")

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
    val chapters: List<ChapterMark>? = null,
    override val claimed: Set<BookField> = emptySet(),
) : LayerRow<BookField>

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
        providerID = this[table.providerId],
        providerRating = this[table.providerRating],
        coverID = this[table.coverId]?.value,
        genres = this[table.genres],
        narrators = this[table.narrators],
        chapters = this[table.chapters],
        claimed = this[table.claimed],
    )

// Nothing outside of this file may write a book layer.
context(_: Transaction)
fun BookMetadata.write(row: BookMetadataRow) {
    val updated = update({ id eq row.book }) { write(it, row) }
    if (updated == 0) insert { write(it, row) }
    reconcileBook(row.book)
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
    stmt[providerId] = row.providerID
    stmt[providerRating] = row.providerRating
    stmt[coverId] = row.coverID
    stmt[genres] = row.genres
    stmt[narrators] = row.narrators
    stmt[chapters] = row.chapters
    stmt[claimed] = row.claimed
}
