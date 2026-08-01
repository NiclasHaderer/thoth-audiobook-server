package io.thoth.server.repositories

import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.models.Author
import io.thoth.models.AuthorDetailed
import io.thoth.models.AuthorUpdate
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.database.access.fetchImage
import io.thoth.server.database.access.getNewImage
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageEntity
import io.thoth.server.database.tables.SeriesTable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

interface AuthorRepository : Repository<AuthorEntity, Author, AuthorDetailed, AuthorUpdate> {
    fun findByName(
        authorName: String,
        libraryId: UUID,
    ): AuthorEntity?

    fun getOrCreate(
        authorName: String,
        libraryId: UUID,
    ): AuthorEntity

    fun create(
        authorName: String,
        libraryId: UUID,
    ): AuthorEntity
}

class AuthorServiceImpl :
    AuthorRepository,
    KoinComponent {
    val metadataAgents by inject<MetadataAgents>()
    val libraryRepository by inject<LibraryRepository>()

    override fun findByName(
        authorName: String,
        libraryId: UUID,
    ): AuthorEntity? =
        transaction {
            AuthorEntity
                .find { (AuthorTable.name ilike escape(authorName)) and (AuthorTable.library eq libraryId) }
                .firstOrNull()
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ) = transaction {
        AuthorEntity.find { AuthorTable.id eq id and (AuthorTable.library eq libraryId) }.firstOrNull()
            ?: throw ErrorResponse.notFound("Author", id)
    }

    override fun search(
        query: String,
        libraryId: UUID,
    ): List<Author> =
        transaction {
            AuthorEntity
                .find { (AuthorTable.name ilike "%${escape(query)}%") and (AuthorTable.library eq libraryId) }
                .orderBy(AuthorTable.name.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toModel() }
        }

    override fun search(query: String): List<Author> =
        transaction {
            AuthorEntity
                .find { AuthorTable.name ilike "%${escape(query)}%" }
                .orderBy(AuthorTable.name.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toModel() }
        }

    override fun getOrCreate(
        authorName: String,
        libraryId: UUID,
    ): AuthorEntity = transaction { findByName(authorName, libraryId) ?: create(authorName, libraryId) }

    override fun create(
        authorName: String,
        libraryId: UUID,
    ): AuthorEntity =
        transaction {
            AuthorEntity.new { name = authorName }.also { it.library = libraryRepository.raw(libraryId) }
        }

    override fun autoMatch(
        id: UUID,
        libraryId: UUID,
    ): Author {
        val (metadataAgent, authorName, region) =
            transaction {
                val library = libraryRepository.raw(libraryId)
                AutoMatchQuery(metadataAgents.forLibrary(library), raw(id, libraryId).name, library.language)
            }
        val result = runBlocking { metadataAgent.getAuthorByName(authorName, region).firstOrNull() }
        val newImage = fetchImage(result?.imageURL)

        return transaction {
            val author = raw(id, libraryId)
            author
                .apply {
                    displayName = result?.name ?: author.displayName
                    provider = result?.id?.provider ?: author.provider
                    providerID = result?.id?.itemID ?: author.providerID
                    biography = result?.biography ?: author.biography
                    website = result?.website ?: author.website
                    bornIn = result?.bornIn ?: author.bornIn
                    birthDate = result?.birthDate ?: author.birthDate
                    deathDate = result?.deathDate ?: author.deathDate
                    imageID = ImageEntity.getNewImage(newImage, currentImageID = imageID, default = imageID)
                }.toModel()
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
            AuthorEntity
                .find { AuthorTable.library eq libraryId }
                .orderBy(AuthorTable.name.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it.toModel() }
        }

    override fun get(
        id: UUID,
        libraryId: UUID,
    ): AuthorDetailed =
        transaction {
            val author = raw(id, libraryId)

            AuthorDetailed.fromModel(
                author = author.toModel(),
                books = author.books.orderBy(BooksTable.title.lowerCase() to SortOrder.ASC).map { it.toModel() },
                series = author.series.orderBy(SeriesTable.title.lowerCase() to SortOrder.ASC).map { it.toModel() },
            )
        }

    override fun sorting(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<UUID> =
        transaction {
            AuthorEntity
                .find { AuthorTable.library eq libraryId }
                .orderBy(AuthorTable.name.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it.id.value }
        }

    override fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
    ): Long =
        transaction {
            AuthorEntity
                .find { AuthorTable.library eq libraryId }
                .orderBy(AuthorTable.name.lowerCase() to order)
                .indexOfFirst { it.id.value == id }
                .takeIf { it >= 0 }
                ?.toLong()
                ?: throw ErrorResponse.notFound("Author", id)
        }

    override fun modify(
        id: UUID,
        libraryId: UUID,
        partial: AuthorUpdate,
    ): Author {
        val newImage = fetchImage(partial.image)
        return transaction {
            val author = raw(id, libraryId)
            author
                .apply {
                    name = partial.name ?: author.name
                    provider = partial.provider ?: author.provider
                    providerID = partial.providerID ?: author.providerID
                    biography = partial.biography ?: author.biography
                    website = partial.website ?: author.website
                    bornIn = partial.bornIn ?: author.bornIn
                    birthDate = partial.birthDate ?: author.birthDate
                    deathDate = partial.deathDate ?: author.deathDate
                    imageID = ImageEntity.getNewImage(newImage, currentImageID = imageID, default = imageID)
                }.toModel()
        }
    }

    override fun total(libraryId: UUID): Long =
        transaction { AuthorEntity.find { AuthorTable.library eq libraryId }.count() }
}
