package io.thoth.auth.interactions

import io.ktor.server.routing.RoutingContext
import io.thoth.auth.models.ThothChangePassword
import io.thoth.auth.thothAuthConfig
import io.thoth.auth.withUserMutation
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.auth.utils.generateJwtPairForUser
import io.thoth.auth.utils.hashPassword
import io.thoth.auth.utils.passwordMatches
import io.thoth.auth.utils.thothPrincipal
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.UUID

interface ThothChangePasswordParams {
    val id: UUID
}

fun RoutingContext.changeUserPassword(
    params: ThothChangePasswordParams,
    passwordChange: ThothChangePassword,
) {
    val principal = thothPrincipal<ThothPrincipal>()
    val config = thothAuthConfig<Any, Any>()

    if (principal.userId != params.id && !config.isAdmin(principal)) {
        throw ErrorResponse.forbidden("Change", "password")
    }

    val user = config.getUserById(params.id) ?: throw ErrorResponse.userError("Could not find user with username")

    config.passwordMeetsRequirements(passwordChange.newPassword).also { (meetsRequirements, message) ->
        if (!meetsRequirements) {
            throw ErrorResponse.userError(message!!)
        }
    }

    if (principal.userId == params.id && !passwordMatches(passwordChange.currentPassword, user)) {
        throw ErrorResponse.userError("Wrong password")
    }

    val newPassword = hashPassword(passwordChange.newPassword)
    val updated = withUserMutation { config.updatePassword(user, newPassword) }
    check(updated.tokenVersion != user.tokenVersion) {
        "updatePassword must increment the user's tokenVersion, otherwise tokens issued before the change stay valid"
    }

    // The bump invalidated every token of this user, including the one used for this request
    if (principal.userId == params.id) call.appendAuthCookies(generateJwtPairForUser(updated, config), config)
}
