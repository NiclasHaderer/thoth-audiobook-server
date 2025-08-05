package io.thoth.auth.interactions

import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.*

interface ThothDeleteUserParams {
    val id: UUID
}

fun RouteHandler.deleteUser(params: ThothDeleteUserParams, body: Unit) {
    val principal = thothPrincipal<ThothPrincipal>()
    val config = thothAuthConfig<Any>()

    if (principal.userId != params.id && !config.isAdmin(principal)) {
        throw ErrorResponse.forbidden("Delete", "user")
    }

    val user = config.getUserById(params.id) ?: throw ErrorResponse.userError("User not found")
    config.deleteUser(user)
}
