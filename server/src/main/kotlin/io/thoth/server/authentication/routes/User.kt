package io.thoth.server.authentication.routes

import io.thoth.generators.openapi.RouteHandler
import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.models.UserModel
import io.thoth.server.authentication.thothPrincipal
import io.thoth.server.database.access.getById
import io.thoth.server.database.tables.User

internal fun RouteHandler.getUser(): UserModel {
    val principal = thothPrincipal()
    return User.getById(principal.userId) ?: throw ErrorResponse.notFound("User", principal.userId)
}

internal fun RouteHandler.deleteUser() {
    val principal = thothPrincipal()
    User.findById(principal.userId)?.delete() ?: throw ErrorResponse.notFound("User", principal.userId)
}
