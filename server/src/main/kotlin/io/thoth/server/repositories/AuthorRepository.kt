package io.thoth.server.repositories

import io.thoth.metadata.MetadataProviders
import io.thoth.metadata.MetadataWrapper
import io.thoth.models.AuthorModel
import io.thoth.models.DetailedAuthorModel
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.api.AuthorApiModel
import io.thoth.server.api.PartialAuthorApiModel
import io.thoth.server.common.extensions.findOne
import io.thoth.server.database.access.getNewImage
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageEntity
import io.thoth.server.database.tables.SeriesTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

interface AuthorRepository :
    Repository<AuthorEntity, AuthorModel, DetailedAuthorModel, PartialAuthorApiModel, AuthorApiModel> {
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
    val metadataProviders by inject<MetadataProviders>()
    val libraryRepository by inject<LibraryRepository>()

    override fun findByName(
        authorName: String,
        libraryId: UUID,
    ): AuthorEntity? =
        transaction {
            AuthorEntity.find { AuthorTable.name like authorName and (AuthorTable.library eq libraryId) }.firstOrNull()
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
    ): List<AuthorModel> =
        transaction {
            AuthorEntity
                .find { AuthorTable.name like "%$query%" and (AuthorTable.library eq libraryId) }
                .orderBy(AuthorTable.name.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toModel() }
        }

    override fun search(query: String): List<AuthorModel> =
        transaction {
            AuthorEntity
                .find { AuthorTable.name like "%$query%" }
                .orderBy(AuthorTable.name.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toModel() }
        }

    override fun getOrCreate(
        authorName: String,
        libraryId: UUID,
    ): AuthorEntity =
        transaction {
            AuthorEntity.findOne { AuthorTable.name like authorName and (AuthorTable.library eq libraryId) }
                ?: create(authorName, libraryId)
        }

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
    ): AuthorModel =
        transaction {
            val library = libraryRepository.raw(libraryId)
            val author = raw(id, libraryId)
            val metadataAgent = MetadataWrapper.fromAgents(library.metadataScanners, metadataProviders)
            val result = runBlocking { metadataAgent.getAuthorByName(author.name, library.language).firstOrNull() }
            author
                .apply {
                    displayName = result?.name
                    provider = result?.id?.provider ?: author.provider
                    providerID = result?.id?.itemID ?: author.providerID
                    biography = result?.biography ?: author.biography
                    website = result?.website ?: author.website
                    bornIn = result?.bornIn ?: author.bornIn
                    birthDate = result?.birthDate ?: author.birthDate
                    deathDate = result?.deathDate ?: author.deathDate
                    imageID = ImageEntity.getNewImage(result?.imageURL, currentImageID = imageID, default = imageID)
                }.toModel()
        }

    override fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<AuthorModel> =
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
    ): DetailedAuthorModel =
        transaction {
            val author = raw(id, libraryId)

            DetailedAuthorModel.fromModel(
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
                .toLong()
        }

    override fun modify(
        id: UUID,
        libraryId: UUID,
        partial: PartialAuthorApiModel,
    ) = transaction {
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
                imageID = ImageEntity.getNewImage(partial.image, currentImageID = imageID, default = imageID)
            }.toModel()
    }

    override fun replace(
        id: UUID,
        libraryId: UUID,
        complete: AuthorApiModel,
    ) = transaction {
        val author = raw(id, libraryId)
        author
            .apply {
                name = complete.name
                provider = complete.provider
                providerID = complete.providerID
                biography = complete.biography
                website = complete.website
                bornIn = complete.bornIn
                birthDate = complete.birthDate
                deathDate = complete.deathDate
                imageID = ImageEntity.getNewImage(complete.image, currentImageID = imageID, default = null)
            }.toModel()
    }

    override fun total(libraryId: UUID): Long =
        transaction { AuthorEntity.find { AuthorTable.library eq libraryId }.count() }
}
