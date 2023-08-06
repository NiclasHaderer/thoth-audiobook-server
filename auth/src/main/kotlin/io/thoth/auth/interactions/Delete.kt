package io.thoth.auth.interactions

import io.thoth.auth.ThothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothDeleteUserParams

fun RouteHandler.deleteUser(
    params: ThothDeleteUserParams,
    body: Unit,
) {
    val principal = thothPrincipal<ThothPrincipal>()
    val user = ThothAuthConfig.getUserById(principal.userId) ?: throw ErrorResponse.userError("User not found")
    ThothAuthConfig.deleteUser(user)
}
