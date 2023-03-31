package io.thoth.auth.routes

import io.thoth.auth.thothPrincipal
import io.thoth.database.tables.User
import io.thoth.openapi.ErrorResponse
import io.thoth.openapi.RouteHandler
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

class PasswordChange(val currentPassword: String, val newPassword: String)

internal fun RouteHandler.changePassword(passwordChange: PasswordChange) {
    if (passwordChange.currentPassword == passwordChange.newPassword) {
        throw ErrorResponse.userError("New password is the same as the current one")
    }

    val principal = thothPrincipal()
    transaction {
        val user = User.findById(principal.userId) ?: throw ErrorResponse.notFound("User", principal.userId)

        val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        if (!encoder.matches(passwordChange.currentPassword, user.passwordHash)) {
            throw ErrorResponse.userError("Old password is wrong.")
        }
        user.passwordHash = encoder.encode(passwordChange.newPassword)
    }
}
