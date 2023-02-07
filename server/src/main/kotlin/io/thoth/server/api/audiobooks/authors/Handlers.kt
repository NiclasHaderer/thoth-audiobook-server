package io.thoth.server.api.audiobooks.authors

import io.ktor.http.*
import io.thoth.common.extensions.syncUriToFile
import io.thoth.database.access.toModel
import io.thoth.database.tables.Author
import io.thoth.database.tables.Image
import io.thoth.models.AuthorModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction


internal fun RouteHandler.patchAuthor(id: AuthorId, patchAuthor: PatchAuthor): AuthorModel {
    val author = transaction { Author.findById(id.id) } ?: serverError(HttpStatusCode.NotFound, "Author not found")
    return transaction {
        author.apply {
            name = patchAuthor.name ?: author.name
            provider = patchAuthor.provider ?: author.provider
            biography = patchAuthor.biography ?: author.biography
            website = patchAuthor.website ?: author.website
            bornIn = patchAuthor.bornIn ?: author.bornIn
            birthDate = patchAuthor.birthDate ?: author.birthDate
            deathDate = patchAuthor.deathDate ?: author.deathDate
            imageID = if (patchAuthor.image != null) {
                Image.new {
                    blob = ExposedBlob(patchAuthor.image.syncUriToFile())
                }.id
            } else {
                author.imageID
            }
        }
        author.toModel()
    }
}
