package io.thoth.server.api

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.access.getDetailedById
import io.thoth.database.access.getMultiple
import io.thoth.database.access.getNewImage
import io.thoth.database.access.positionOf
import io.thoth.database.access.toModel
import io.thoth.database.tables.Author
import io.thoth.database.tables.Image
import io.thoth.database.tables.TAuthors
import io.thoth.models.AuthorModel
import io.thoth.models.DetailedAuthorModel
import io.thoth.models.NamedId
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.put
import io.thoth.openapi.serverError
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction

fun Routing.authorRouting() {
    // TODO restrict to library
    get<Api.Libraries.Id.Authors.All, PaginatedResponse<AuthorModel>> { (limit, offset) ->
        transaction {
            val books = Author.getMultiple(limit, offset)
            val seriesCount = Author.count()
            PaginatedResponse(books, total = seriesCount, offset = offset, limit = limit)
        }
    }
    get<Api.Libraries.Id.Authors.Sorting, List<UUID>> { (limit, offset) ->
        transaction { Author.getMultiple(limit, offset).map { it.id } }
    }

    get<Api.Libraries.Id.Authors.Id.Position, Position> {
        transaction {
            val sortOrder = Author.positionOf(it.id) ?: serverError(HttpStatusCode.NotFound, "Author was not found")
            Position(sortIndex = sortOrder, id = it.id, order = Position.Order.ASC)
        }
    }

    get<Api.Libraries.Id.Authors.Id, DetailedAuthorModel> { (id) ->
        transaction { Author.getDetailedById(id) ?: serverError(HttpStatusCode.NotFound, "Author was not found") }
    }

    get<Api.Libraries.Id.Authors.Autocomplete, List<NamedId>> { (name) ->
        transaction {
            Author.find { TAuthors.name like "%$name%" }
                .orderBy(TAuthors.name.lowerCase() to SortOrder.ASC)
                .limit(30)
                .map { NamedId(it.id.value, it.name) }
        }
    }

    patch<Api.Libraries.Id.Authors.Id, PatchAuthor, AuthorModel> { id, patchAuthor ->
        transaction {
            val author = Author.findById(id.authorId) ?: serverError(HttpStatusCode.NotFound, "Author not found")
            author
                .apply {
                    name = patchAuthor.name ?: author.name
                    provider = patchAuthor.provider ?: author.provider
                    providerID = patchAuthor.providerID ?: author.providerID
                    biography = patchAuthor.biography ?: author.biography
                    website = patchAuthor.website ?: author.website
                    bornIn = patchAuthor.bornIn ?: author.bornIn
                    birthDate = patchAuthor.birthDate ?: author.birthDate
                    deathDate = patchAuthor.deathDate ?: author.deathDate
                    imageID = Image.getNewImage(patchAuthor.image, currentImageID = imageID, default = imageID)
                }
                .toModel()
        }
    }

    put<Api.Libraries.Id.Authors.Id, PutAuthor, AuthorModel> { id, postAuthor ->
        transaction {
            val author = Author.findById(id.authorId) ?: serverError(HttpStatusCode.NotFound, "Author not found")
            author
                .apply {
                    name = postAuthor.name
                    provider = postAuthor.provider
                    providerID = postAuthor.providerID
                    biography = postAuthor.biography
                    website = postAuthor.website
                    bornIn = postAuthor.bornIn
                    birthDate = postAuthor.birthDate
                    deathDate = postAuthor.deathDate
                    imageID = Image.getNewImage(postAuthor.image, currentImageID = imageID, default = null)
                }
                .toModel()
        }
    }
}
