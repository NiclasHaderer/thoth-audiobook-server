package io.thoth.auth.interactions

import io.thoth.auth.models.ThothChangePassword
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.hashPassword
import io.thoth.auth.utils.passwordMatches
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.RouteHandler
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.*

interface ThothChangePasswordParams {
    val id: UUID
}

fun RouteHandler.changeUserPassword(
    params: ThothChangePasswordParams,
    passwordChange: ThothChangePassword,
) {

    val principal = thothPrincipal<ThothPrincipal>()
    val config = thothAuthConfig<Any>()


    if (principal.userId != params.id && !config.isAdmin(principal)) {
        throw ErrorResponse.forbidden("Change", "password")
    }

    val user = config.getUserById(params.id) ?: throw ErrorResponse.userError("Could not find user with username")

    config.passwordMeetsRequirements(passwordChange.newPassword).also { (meetsRequirements, message) ->
        if (!meetsRequirements) {
            throw ErrorResponse.userError(message!!)
        }
    }

    if (!passwordMatches(passwordChange.currentPassword, user)) {
        throw ErrorResponse.userError("Wrong password")
    }

    val newPassword = hashPassword(passwordChange.newPassword)
    config.updatePassword(user, newPassword)
}
