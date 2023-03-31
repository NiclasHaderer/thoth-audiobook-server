package io.thoth.auth.routes

import io.thoth.auth.thothPrincipal
import io.thoth.database.access.getById
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.ErrorResponse
import io.thoth.openapi.RouteHandler

internal fun RouteHandler.getUser(): UserModel {
    val principal = thothPrincipal()
    return User.getById(principal.userId) ?: throw ErrorResponse.notFound("User", principal.userId)
}

internal fun RouteHandler.deleteUser() {
    val principal = thothPrincipal()
    User.findById(principal.userId)?.delete() ?: throw ErrorResponse.notFound("User", principal.userId)
}
