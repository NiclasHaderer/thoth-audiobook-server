package io.thoth.auth.interactions

import io.thoth.auth.AuthConfig
import io.thoth.auth.PasswordChange
import io.thoth.auth.encodePassword
import io.thoth.auth.passwordMatches
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

fun RouteHandler.changePassword(
    passwordChange: PasswordChange,
) {

    // TODO get userId from JWT
    val user = AuthConfig.getUserById(userId) ?: throw ErrorResponse.userError("Could not find user with username")

    if (!passwordMatches(passwordChange.currentPassword, user)) {
        throw ErrorResponse.userError("Wrong password")
    }

    val newPassword = encodePassword(passwordChange.newPassword)
    AuthConfig.updatePassword(user, newPassword)
}
