package io.thoth.server.repositories

import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.models.Author
import io.thoth.models.AuthorDetailed
import io.thoth.models.AuthorUpdate
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.ImageDownloader
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorRow
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.SeriesAuthorTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.booksToModels
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.seriesToModels
import io.thoth.server.database.tables.toAuthorRow
import io.thoth.server.database.tables.toBookRow
import io.thoth.server.database.tables.toSeriesRow
import io.thoth.server.database.tables.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

interface AuthorRepository : Repository<AuthorRow, Author, AuthorDetailed, AuthorUpdate> {
    fun findByName(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow?

    fun getOrCreate(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow

    fun create(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow
}

class AuthorServiceImpl :
    AuthorRepository,
    KoinComponent {
    val metadataAgents by inject<MetadataAgents>()
    val libraryRepository by inject<LibraryRepository>()
    private val imageDownloader by inject<ImageDownloader>()

    override fun findByName(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow? =
        transaction {
            AuthorTable
                .selectAll()
                .where { namedExactly(authorName) and (AuthorTable.library eq libraryId) }
                .firstOrNull()
                ?.toAuthorRow()
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ): AuthorRow =
        transaction {
            AuthorTable
                .selectAll()
                .where { AuthorTable.id eq id and (AuthorTable.library eq libraryId) }
                .firstOrNull()
                ?.toAuthorRow()
                ?: throw ErrorResponse.notFound("Author", id)
        }

    override fun search(
        query: String,
        libraryId: UUID,
    ): List<Author> =
        transaction {
            AuthorTable
                .selectAll()
                .where { matchesName(query) and (AuthorTable.library eq libraryId) }
                .orderBy(AuthorTable.displayedName.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toAuthorRow().toModel() }
        }

    override fun search(query: String): List<Author> =
        transaction {
            AuthorTable
                .selectAll()
                .where { matchesName(query) }
                .orderBy(AuthorTable.displayedName.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toAuthorRow().toModel() }
        }

    override fun getOrCreate(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow = transaction { findByName(authorName, libraryId) ?: create(authorName, libraryId) }

    override fun create(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow =
        transaction {
            val row =
                AuthorRow(
                    id = UUID.randomUUID(),
                    name = authorName,
                    displayName = null,
                    biography = null,
                    website = null,
                    birthDate = null,
                    bornIn = null,
                    deathDate = null,
                    provider = null,
                    providerID = null,
                    imageID = null,
                    library = libraryId,
                )
            AuthorTable.insert(row)
            row
        }

    override fun autoMatch(
        id: UUID,
        libraryId: UUID,
    ): Author {
        val (metadataAgent, authorName, region) =
            transaction {
                val library = libraryRepository.raw(libraryId)
                AutoMatchQuery(metadataAgents.forLibrary(library), raw(id, libraryId).displayedName, library.language)
            }
        val result = runBlocking { metadataAgent.getAuthorByName(authorName, region).firstOrNull() }
        val newImage = imageDownloader.download(result?.imageURL)

        return transaction {
            val author = raw(id, libraryId)
            val updated =
                author.copy(
                    displayName = result?.name ?: author.displayName,
                    provider = result?.id?.provider ?: author.provider,
                    providerID = result?.id?.itemID ?: author.providerID,
                    biography = result?.biography ?: author.biography,
                    website = result?.website ?: author.website,
                    bornIn = result?.bornIn ?: author.bornIn,
                    birthDate = result?.birthDate ?: author.birthDate,
                    deathDate = result?.deathDate ?: author.deathDate,
                    imageID = getOrCreateImage(newImage, currentImageID = author.imageID),
                )
            AuthorTable.update(updated)
            updated.toModel()
        }
    }

    private data class AutoMatchQuery(
        val metadataAgent: MetadataAgent,
        val authorName: String,
        val region: String,
    )

    override fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<Author> =
        transaction {
            AuthorTable
                .selectAll()
                .where { AuthorTable.library eq libraryId }
                .orderBy(AuthorTable.displayedName.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it.toAuthorRow().toModel() }
        }

    override fun get(
        id: UUID,
        libraryId: UUID,
    ): AuthorDetailed =
        transaction {
            val author = raw(id, libraryId)

            val books =
                (AuthorBookTable innerJoin BooksTable)
                    .selectAll()
                    .where { AuthorBookTable.authors eq id }
                    .orderBy(BooksTable.displayedTitle.lowerCase() to SortOrder.ASC)
                    .map { it.toBookRow() }
            val series =
                (SeriesAuthorTable innerJoin SeriesTable)
                    .selectAll()
                    .where { SeriesAuthorTable.author eq id }
                    .orderBy(SeriesTable.displayedTitle.lowerCase() to SortOrder.ASC)
                    .map { it.toSeriesRow() }

            AuthorDetailed.fromModel(
                author = author.toModel(),
                books = booksToModels(books),
                series = seriesToModels(series),
            )
        }

    override fun sorting(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<UUID> =
        transaction {
            AuthorTable
                .selectAll()
                .where { AuthorTable.library eq libraryId }
                .orderBy(AuthorTable.displayedName.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it[AuthorTable.id].value }
        }

    override fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
    ): Long =
        transaction {
            val name = raw(id, libraryId).displayedName.lowercase()
            AuthorTable
                .selectAll()
                .where {
                    val precedes =
                        if (order == SortOrder.ASC) {
                            AuthorTable.displayedName.lowerCase() less name
                        } else {
                            AuthorTable.displayedName.lowerCase() greater name
                        }
                    precedes and (AuthorTable.library eq libraryId)
                }.count()
        }

    override fun modify(
        id: UUID,
        libraryId: UUID,
        partial: AuthorUpdate,
    ): Author {
        val currentImage = raw(id, libraryId).imageID
        val newImage = imageDownloader.download(partial.image?.takeUnless { it == currentImage?.toString() })
        return transaction {
            val author = raw(id, libraryId)
            val updated =
                author.copy(
                    displayName = partial.name ?: author.displayName,
                    provider = partial.provider ?: author.provider,
                    providerID = partial.providerID ?: author.providerID,
                    biography = partial.biography ?: author.biography,
                    website = partial.website ?: author.website,
                    bornIn = partial.bornIn ?: author.bornIn,
                    birthDate = partial.birthDate ?: author.birthDate,
                    deathDate = partial.deathDate ?: author.deathDate,
                    imageID = getOrCreateImage(newImage, currentImageID = author.imageID),
                )
            AuthorTable.update(updated)
            updated.toModel()
        }
    }

    override fun total(libraryId: UUID): Long =
        transaction { AuthorTable.selectAll().where { AuthorTable.library eq libraryId }.count() }
}

// A rename only moves displayName, so the files keep matching on name. A later scan whose tags carry the new
// spelling has to land on the same author too, which is why both columns are compared.
private fun namedExactly(name: String): Op<Boolean> = eitherName(escape(name))

private fun matchesName(query: String): Op<Boolean> = eitherName("%${escape(query)}%")

private fun eitherName(pattern: String): Op<Boolean> =
    (AuthorTable.name ilike pattern) or (AuthorTable.displayName ilike pattern)
