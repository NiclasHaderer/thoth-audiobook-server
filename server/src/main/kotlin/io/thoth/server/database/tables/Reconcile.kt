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
    val preferFile = prefersFile(BookTable, bookId) ?: return
    val user = BookUserMetadataTable.layer(bookId)
    val agent = BookAgentMetadataTable.layer(bookId)
    val file = BookFileMetadataTable.layer(bookId)

    fun <T> pick(
        field: BookField,
        get: BookMetadataRow.() -> T?,
    ) = resolve(field, get, user, agent, file, preferFile)

    BookTable.update({ BookTable.id eq bookId }) {
        it[title] = pick(BookField.TITLE) { title } ?: named("Book", bookId)
        it[releaseDate] = pick(BookField.RELEASE_DATE) { releaseDate }
        it[publisher] = pick(BookField.PUBLISHER) { publisher }
        it[language] = pick(BookField.LANGUAGE) { language }
        it[description] = pick(BookField.DESCRIPTION) { description }
        it[isbn] = pick(BookField.ISBN) { isbn }
        it[provider] = pick(BookField.PROVIDER) { provider }
        it[providerId] = pick(BookField.PROVIDER_ID) { providerID }
        it[providerRating] = pick(BookField.PROVIDER_RATING) { providerRating }
        it[coverId] = pick(BookField.COVER_ID) { coverID }
        it[genres] = pick(BookField.GENRES) { genres }
        it[narrators] = pick(BookField.NARRATORS) { narrators }
        it[authorsFrom] =
            resolveLayer(
                BookField.AUTHORS in user.claimed,
                BookField.AUTHORS in agent.claimed,
                namedByFileLayer(AuthorBookTable.book, AuthorBookTable.addedBy, bookId),
                preferFile,
            )
        it[seriesFrom] =
            resolveLayer(
                BookField.SERIES in user.claimed,
                BookField.SERIES in agent.claimed,
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

    fun <T> pick(
        field: AuthorField,
        get: AuthorMetadataRow.() -> T?,
    ) = resolve(field, get, user, agent, file, preferFile)

    AuthorTable.update({ AuthorTable.id eq authorId }) {
        it[name] = pick(AuthorField.NAME) { name } ?: named("Author", authorId)
        it[biography] = pick(AuthorField.BIOGRAPHY) { biography }
        it[website] = pick(AuthorField.WEBSITE) { website }
        it[birthDate] = pick(AuthorField.BIRTH_DATE) { birthDate }
        it[bornIn] = pick(AuthorField.BORN_IN) { bornIn }
        it[deathDate] = pick(AuthorField.DEATH_DATE) { deathDate }
        it[provider] = pick(AuthorField.PROVIDER) { provider }
        it[providerId] = pick(AuthorField.PROVIDER_ID) { providerID }
        it[imageId] = pick(AuthorField.IMAGE_ID) { imageID }
    }
}

context(_: Transaction)
fun reconcileSeries(seriesId: UUID) {
    val preferFile = prefersFile(SeriesTable, seriesId) ?: return
    val user = SeriesUserMetadataTable.layer(seriesId)
    val agent = SeriesAgentMetadataTable.layer(seriesId)
    val file = SeriesFileMetadataTable.layer(seriesId)

    fun <T> pick(
        field: SeriesField,
        get: SeriesMetadataRow.() -> T?,
    ) = resolve(field, get, user, agent, file, preferFile)

    SeriesTable.update({ SeriesTable.id eq seriesId }) {
        it[title] = pick(SeriesField.TITLE) { title } ?: named("Series", seriesId)
        it[totalBooks] = pick(SeriesField.TOTAL_BOOKS) { totalBooks }
        it[primaryWorks] = pick(SeriesField.PRIMARY_WORKS) { primaryWorks }
        it[description] = pick(SeriesField.DESCRIPTION) { description }
        it[provider] = pick(SeriesField.PROVIDER) { provider }
        it[providerId] = pick(SeriesField.PROVIDER_ID) { providerID }
        it[coverId] = pick(SeriesField.COVER_ID) { coverID }
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
