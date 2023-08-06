package io.thoth.auth.interactions

import io.thoth.auth.models.PasswordChange
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.hashPassword
import io.thoth.auth.utils.passwordMatches
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse

interface ThothChangePasswordParams

fun RouteHandler.changePassword(
    params: ThothChangePasswordParams,
    passwordChange: PasswordChange,
) {
    val principal = thothPrincipal<ThothPrincipal>()
    val config = thothAuthConfig()

    val user =
        config.getUserById(principal.userId) ?: throw ErrorResponse.userError("Could not find user with username")

    if (!passwordMatches(passwordChange.currentPassword, user)) {
        throw ErrorResponse.userError("Wrong password")
    }

    val newPassword = hashPassword(passwordChange.newPassword)
    config.updatePassword(user, newPassword)
}
