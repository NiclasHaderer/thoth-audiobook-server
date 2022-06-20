package io.thoth.server.api.audiobooks.books

import io.ktor.http.*
import io.thoth.models.BookModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError

internal suspend fun RouteHandler.patchBook(id: BookId, patchBook: PatchBook): BookModel {
    serverError(HttpStatusCode.NotImplemented, "Book modification is not yet supported")
}
