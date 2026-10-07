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
fun bookLayers(bookId: UUID): Layers<BookMetadataRow, BookField>? {
    val preferFile = prefersFile(BookTable, bookId) ?: return null
    return Layers(
        BookUserMetadataTable.layer(bookId),
        BookAgentMetadataTable.layer(bookId),
        BookFileMetadataTable.layer(bookId),
        preferFile,
    )
}

context(_: Transaction)
fun authorLayers(authorId: UUID): Layers<AuthorMetadataRow, AuthorField>? {
    val preferFile = prefersFile(AuthorTable, authorId) ?: return null
    return Layers(
        AuthorUserMetadataTable.layer(authorId),
        AuthorAgentMetadataTable.layer(authorId),
        AuthorFileMetadataTable.layer(authorId),
        preferFile,
    )
}

context(_: Transaction)
fun seriesLayers(seriesId: UUID): Layers<SeriesMetadataRow, SeriesField>? {
    val preferFile = prefersFile(SeriesTable, seriesId) ?: return null
    return Layers(
        SeriesUserMetadataTable.layer(seriesId),
        SeriesAgentMetadataTable.layer(seriesId),
        SeriesFileMetadataTable.layer(seriesId),
        preferFile,
    )
}

context(_: Transaction)
fun reconcileBook(bookId: UUID) {
    val layers = bookLayers(bookId) ?: return

    BookTable.update({ BookTable.id eq bookId }) {
        it[title] = layers.resolve(BookField.TITLE) { title } ?: named("Book", bookId)
        it[releaseDate] = layers.resolve(BookField.RELEASE_DATE) { releaseDate }
        it[publisher] = layers.resolve(BookField.PUBLISHER) { publisher }
        it[language] = layers.resolve(BookField.LANGUAGE) { language }
        it[description] = layers.resolve(BookField.DESCRIPTION) { description }
        it[isbn] = layers.resolve(BookField.ISBN) { isbn }
        it[provider] = layers.resolve(BookField.PROVIDER) { provider }
        it[providerId] = layers.resolve(BookField.PROVIDER_ID) { providerID }
        it[providerRating] = layers.resolve(BookField.PROVIDER_RATING) { providerRating }
        it[coverId] = layers.resolve(BookField.COVER_ID) { coverID }
        it[genres] = layers.resolve(BookField.GENRES) { genres }
        it[narrators] = layers.resolve(BookField.NARRATORS) { narrators }
        it[authorsFrom] =
            resolveLayer(
                BookField.AUTHORS in layers.user.claimed,
                BookField.AUTHORS in layers.agent.claimed,
                namedByFileLayer(AuthorBookTable.book, AuthorBookTable.addedBy, bookId),
                layers.preferFile,
            )
        it[seriesFrom] =
            resolveLayer(
                BookField.SERIES in layers.user.claimed,
                BookField.SERIES in layers.agent.claimed,
                namedByFileLayer(SeriesBookTable.book, SeriesBookTable.addedBy, bookId),
                layers.preferFile,
            )
    }
}

context(_: Transaction)
fun reconcileAuthor(authorId: UUID) {
    val layers = authorLayers(authorId) ?: return

    AuthorTable.update({ AuthorTable.id eq authorId }) {
        it[name] = layers.resolve(AuthorField.NAME) { name } ?: named("Author", authorId)
        it[biography] = layers.resolve(AuthorField.BIOGRAPHY) { biography }
        it[website] = layers.resolve(AuthorField.WEBSITE) { website }
        it[birthDate] = layers.resolve(AuthorField.BIRTH_DATE) { birthDate }
        it[bornIn] = layers.resolve(AuthorField.BORN_IN) { bornIn }
        it[deathDate] = layers.resolve(AuthorField.DEATH_DATE) { deathDate }
        it[provider] = layers.resolve(AuthorField.PROVIDER) { provider }
        it[providerId] = layers.resolve(AuthorField.PROVIDER_ID) { providerID }
        it[imageId] = layers.resolve(AuthorField.IMAGE_ID) { imageID }
    }
}

context(_: Transaction)
fun reconcileSeries(seriesId: UUID) {
    val layers = seriesLayers(seriesId) ?: return

    SeriesTable.update({ SeriesTable.id eq seriesId }) {
        it[title] = layers.resolve(SeriesField.TITLE) { title } ?: named("Series", seriesId)
        it[totalBooks] = layers.resolve(SeriesField.TOTAL_BOOKS) { totalBooks }
        it[primaryWorks] = layers.resolve(SeriesField.PRIMARY_WORKS) { primaryWorks }
        it[description] = layers.resolve(SeriesField.DESCRIPTION) { description }
        it[provider] = layers.resolve(SeriesField.PROVIDER) { provider }
        it[providerId] = layers.resolve(SeriesField.PROVIDER_ID) { providerID }
        it[coverId] = layers.resolve(SeriesField.COVER_ID) { coverID }
    }
}

context(_: Transaction)
fun reconcileLibrary(libraryId: UUID) {
    idsIn(BookTable, libraryId).forEach { reconcileBook(it) }
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
        .join(LibraryTable, JoinType.INNER, owner.library, LibraryTable.id)
        .select(LibraryTable.preferEmbeddedMetadata)
        .where { owner.id eq ownerId }
        .firstOrNull()
        ?.get(LibraryTable.preferEmbeddedMetadata)

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
