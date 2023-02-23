package io.thoth.server.api.audiobooks.authors

import io.ktor.http.*
import io.thoth.database.access.getNewImage
import io.thoth.database.access.toModel
import io.thoth.database.tables.Author
import io.thoth.database.tables.Image
import io.thoth.models.AuthorModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction


internal fun RouteHandler.patchAuthor(id: AuthorId, patchAuthor: PatchAuthor): AuthorModel = transaction {
    val author = Author.findById(id.id) ?: serverError(HttpStatusCode.NotFound, "Author not found")
    author.apply {
        name = patchAuthor.name ?: author.name
        provider = patchAuthor.provider ?: author.provider
        providerID = patchAuthor.providerID ?: author.providerID
        biography = patchAuthor.biography ?: author.biography
        website = patchAuthor.website ?: author.website
        bornIn = patchAuthor.bornIn ?: author.bornIn
        birthDate = patchAuthor.birthDate ?: author.birthDate
        deathDate = patchAuthor.deathDate ?: author.deathDate
        imageID = Image.getNewImage(patchAuthor.image, currentImageID = imageID, default = imageID)
    }.toModel()
}


internal fun RouteHandler.postAuthor(id: AuthorId, postAuthor: PostAuthor): AuthorModel = transaction {
    val author = Author.findById(id.id) ?: serverError(HttpStatusCode.NotFound, "Author not found")
    author.apply {
        name = postAuthor.name
        provider = postAuthor.provider
        providerID = postAuthor.providerID
        biography = postAuthor.biography
        website = postAuthor.website
        bornIn = postAuthor.bornIn
        birthDate = postAuthor.birthDate
        deathDate = postAuthor.deathDate
        imageID = Image.getNewImage(postAuthor.image, currentImageID = imageID, default = null)
    }.toModel()
}