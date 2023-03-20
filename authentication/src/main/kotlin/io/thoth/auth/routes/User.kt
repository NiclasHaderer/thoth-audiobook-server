package io.thoth.auth.routes

import io.ktor.http.*
import io.thoth.auth.thothPrincipal
import io.thoth.database.access.getById
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.RouteHandler
import io.thoth.openapi.serverError

internal fun RouteHandler.getUser(): UserModel {
    val principal = thothPrincipal()
    return User.getById(principal.userId) ?: serverError(HttpStatusCode.NotFound, "Could not find user")
}

internal fun RouteHandler.deleteUser() {
    val principal = thothPrincipal()
    User.findById(principal.userId)?.delete() ?: serverError(HttpStatusCode.NotFound, "Could not find user")
}
