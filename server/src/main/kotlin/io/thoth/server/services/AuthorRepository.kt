package io.thoth.server.services

import io.thoth.database.access.getNewImage
import io.thoth.database.access.toModel
import io.thoth.database.tables.Author
import io.thoth.database.tables.Image
import io.thoth.database.tables.TAuthors
import io.thoth.database.tables.TBooks
import io.thoth.database.tables.TSeries
import io.thoth.models.AuthorModel
import io.thoth.models.DetailedAuthorModel
import io.thoth.openapi.ErrorResponse
import io.thoth.server.api.AuthorApiModel
import io.thoth.server.api.PartialAuthorApiModel
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction

interface AuthorRepository :
    Repository<Author, AuthorModel, DetailedAuthorModel, PartialAuthorApiModel, AuthorApiModel>

class AuthorServiceImpl : AuthorRepository {

    override fun raw(id: UUID, libraryId: UUID) = transaction {
        Author.find { TAuthors.id eq id and (TAuthors.library eq libraryId) }.firstOrNull()
            ?: throw ErrorResponse.notFound("Author", id)
    }

    override fun search(query: String, libraryId: UUID): List<AuthorModel> = transaction {
        Author.find { TAuthors.name like "%$query%" and (TAuthors.library eq libraryId) }
            .orderBy(TAuthors.name.lowerCase() to SortOrder.ASC)
            .limit(searchLimit)
            .map { it.toModel() }
    }

    override fun search(query: String): List<AuthorModel> = transaction {
        Author.find { TAuthors.name like "%$query%" }
            .orderBy(TAuthors.name.lowerCase() to SortOrder.ASC)
            .limit(searchLimit)
            .map { it.toModel() }
    }

    override fun getAll(libraryId: UUID, order: SortOrder, limit: Int, offset: Long): List<AuthorModel> = transaction {
        Author.find { TAuthors.library eq libraryId }
            .orderBy(TAuthors.name.lowerCase() to order)
            .limit(limit, offset)
            .map { it.toModel() }
    }

    override fun get(id: UUID, libraryId: UUID): DetailedAuthorModel = transaction {
        val author = raw(id, libraryId)

        DetailedAuthorModel.fromModel(
            author = author.toModel(),
            books = author.books.orderBy(TBooks.title.lowerCase() to SortOrder.ASC).map { it.toModel() },
            series = author.series.orderBy(TSeries.title.lowerCase() to SortOrder.ASC).map { it.toModel() },
        )
    }

    override fun sorting(libraryId: UUID, order: SortOrder, limit: Int, offset: Long): List<UUID> = transaction {
        Author.find { TAuthors.library eq libraryId }
            .orderBy(TAuthors.name.lowerCase() to order)
            .limit(limit, offset)
            .map { it.id.value }
    }

    override fun position(id: UUID, libraryId: UUID, order: SortOrder): Long = transaction {
        Author.find { TAuthors.library eq libraryId }
            .orderBy(TAuthors.name.lowerCase() to order)
            .indexOfFirst { it.id.value == id }
            .toLong()
    }

    override fun modify(id: UUID, libraryId: UUID, partial: PartialAuthorApiModel) = transaction {
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
                imageID = Image.getNewImage(partial.image, currentImageID = imageID, default = imageID)
            }
            .toModel()
    }

    override fun replace(id: UUID, libraryId: UUID, complete: AuthorApiModel) = transaction {
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
                imageID = Image.getNewImage(complete.image, currentImageID = imageID, default = null)
            }
            .toModel()
    }

    override fun total(libraryId: UUID): Long = transaction { Author.find { TAuthors.library eq libraryId }.count() }
}
