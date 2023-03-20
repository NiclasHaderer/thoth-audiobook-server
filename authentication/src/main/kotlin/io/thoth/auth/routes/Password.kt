package io.thoth.auth.routes

import io.thoth.auth.thothPrincipal
import io.thoth.openapi.RouteHandler
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction

class PasswordChange(val currentPassword: String, val newPassword: String)

internal fun RouteHandler.changePassword(passwordChange: PasswordChange) {
    if (passwordChange.currentPassword == passwordChange.newPassword) {
        serverError(io.ktor.http.HttpStatusCode.BadRequest, "New password is the same as the current one")
    }

    val principal = thothPrincipal()
    transaction {
        val user =
            io.thoth.database.tables.User.findById(principal.userId)
                ?: serverError(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    "Could not find user with id ${principal.userId}",
                )

        val encoder = org.springframework.security.crypto.argon2.Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        if (!encoder.matches(passwordChange.currentPassword, user.passwordHash)) {
            serverError(io.ktor.http.HttpStatusCode.BadRequest, "Could change password. Old password is wrong.")
        }
        user.passwordHash = encoder.encode(passwordChange.newPassword)
    }
}
