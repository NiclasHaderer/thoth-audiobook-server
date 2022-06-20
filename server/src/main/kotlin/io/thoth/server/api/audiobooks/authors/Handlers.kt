package io.thoth.server.api.audiobooks.authors

import io.ktor.http.*
import io.thoth.models.AuthorModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError


internal suspend fun RouteHandler.patchAuthor(id: AuthorId, patchAuthor: PatchAuthor): AuthorModel {
    serverError(HttpStatusCode.NotImplemented, "Author modification is not yet supported")
}
