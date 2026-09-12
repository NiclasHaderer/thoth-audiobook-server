package io.thoth.server.database.tables

import io.thoth.openapi.ktor.errors.ErrorResponse
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

context(_: Transaction)
fun reconcileBook(bookId: UUID) {
    val preferFile = prefersFile(BooksTable, bookId) ?: return
    val user = BookUserMetadataTable.layer(bookId)
    val agent = BookAgentMetadataTable.layer(bookId)
    val file = BookFileMetadataTable.layer(bookId)

    fun <T> pick(field: BookMetadataRow.() -> T?) = resolve(user.field(), agent.field(), file.field(), preferFile)

    BooksTable.update({ BooksTable.id eq bookId }) {
        it[title] = pick { title } ?: named("Book", bookId)
        it[releaseDate] = pick { releaseDate }
        it[publisher] = pick { publisher }
        it[language] = pick { language }
        it[description] = pick { description }
        it[isbn] = pick { isbn }
        it[provider] = pick { provider }
        it[providerID] = pick { providerID }
        it[providerRating] = pick { providerRating }
        it[coverID] = pick { coverID }
        it[genres] = pick { genres }
        it[narrators] = pick { narrators }
        it[authorsFrom] =
            resolveLayer(
                user.authorsSet,
                agent.authorsSet,
                namedByFileLayer(AuthorBookTable.book, AuthorBookTable.addedBy, bookId),
                preferFile,
            )
        it[seriesFrom] =
            resolveLayer(
                user.seriesSet,
                agent.seriesSet,
                namedByFileLayer(SeriesBookTable.book, SeriesBookTable.addedBy, bookId),
                preferFile,
            )
    }
}

context(_: Transaction)
fun reconcileAuthor(authorId: UUID) {
    val preferFile = prefersFile(AuthorTable, authorId) ?: return
    val user = AuthorUserMetadataTable.layer(authorId)
    val agent = AuthorAgentMetadataTable.layer(authorId)
    val file = AuthorFileMetadataTable.layer(authorId)

    fun <T> pick(field: AuthorMetadataRow.() -> T?) = resolve(user.field(), agent.field(), file.field(), preferFile)

    AuthorTable.update({ AuthorTable.id eq authorId }) {
        it[name] = pick { name } ?: named("Author", authorId)
        it[biography] = pick { biography }
        it[website] = pick { website }
        it[birthDate] = pick { birthDate }
        it[bornIn] = pick { bornIn }
        it[deathDate] = pick { deathDate }
        it[provider] = pick { provider }
        it[providerID] = pick { providerID }
        it[imageID] = pick { imageID }
    }
}

context(_: Transaction)
fun reconcileSeries(seriesId: UUID) {
    val preferFile = prefersFile(SeriesTable, seriesId) ?: return
    val user = SeriesUserMetadataTable.layer(seriesId)
    val agent = SeriesAgentMetadataTable.layer(seriesId)
    val file = SeriesFileMetadataTable.layer(seriesId)

    fun <T> pick(field: SeriesMetadataRow.() -> T?) = resolve(user.field(), agent.field(), file.field(), preferFile)

    SeriesTable.update({ SeriesTable.id eq seriesId }) {
        it[title] = pick { title } ?: named("Series", seriesId)
        it[totalBooks] = pick { totalBooks }
        it[primaryWorks] = pick { primaryWorks }
        it[description] = pick { description }
        it[provider] = pick { provider }
        it[providerID] = pick { providerID }
        it[coverID] = pick { coverID }
    }
}

context(_: Transaction)
fun reconcileLibrary(libraryId: UUID) {
    idsIn(BooksTable, libraryId).forEach { reconcileBook(it) }
    idsIn(AuthorTable, libraryId).forEach { reconcileAuthor(it) }
    idsIn(SeriesTable, libraryId).forEach { reconcileSeries(it) }
}

private fun named(
    what: String,
    id: UUID,
): Nothing = throw ErrorResponse.internalError("$what $id has no name in any metadata layer")

context(_: Transaction)
private fun prefersFile(
    owner: LibraryEntityTable,
    ownerId: UUID,
): Boolean? =
    owner
        .join(LibrariesTable, JoinType.INNER, owner.library, LibrariesTable.id)
        .select(LibrariesTable.preferEmbeddedMetadata)
        .where { owner.id eq ownerId }
        .firstOrNull()
        ?.get(LibrariesTable.preferEmbeddedMetadata)

context(_: Transaction)
private fun idsIn(
    owner: LibraryEntityTable,
    libraryId: UUID,
): List<UUID> = owner.select(owner.id).where { owner.library eq libraryId }.map { it[owner.id].value }

context(_: Transaction)
private fun namedByFileLayer(
    book: Column<EntityID<UUID>>,
    addedBy: Column<MetadataLayer>,
    bookId: UUID,
): Boolean =
    !book.table
        .select(book)
        .where { (book eq bookId) and (addedBy eq MetadataLayer.FILE) }
        .empty()
