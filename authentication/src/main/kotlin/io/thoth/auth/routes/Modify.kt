package io.thoth.auth.routes

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.auth.thothPrincipal
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.post
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder


internal fun Route.modifyUser() = patch<IdRoute, EditUser, UserModel> { userID, postModel ->

    val principal = thothPrincipal()

    transaction {
        val user = User.findById(userID.id) ?: serverError(
            HttpStatusCode.BadRequest,
            "Could not find user with id $userID"
        )

        if (user.admin && principal.userId != user.id.value) {
            serverError(HttpStatusCode.BadRequest, "A admin user cannot be edited")
        }

        user.edit = postModel.edit
        user.admin = postModel.admin
        user.username = postModel.username

        if (postModel.password != null) {
            val encoder = Argon2PasswordEncoder()
            val encodedPassword = encoder.encode(postModel.password)
            user.passwordHash = encodedPassword
        }

        user
    }.toModel().toPublicModel()
}


internal fun Route.changePassword() = post<Unit, PasswordChange, Unit> { _, passwordChange ->
    if (passwordChange.currentPassword == passwordChange.newPassword) {
        serverError(HttpStatusCode.BadRequest, "New password is the same as the current one")
    }

    val principal = thothPrincipal()
    transaction {
        val user = User.findById(principal.userId) ?: serverError(
            HttpStatusCode.BadRequest,
            "Could not find user with id ${principal.userId}"
        )

        val encoder = Argon2PasswordEncoder()
        if (!encoder.matches(passwordChange.currentPassword, user.passwordHash)) {
            serverError(HttpStatusCode.BadRequest, "Could change password. Old password is wrong.")
        }
        user.passwordHash = encoder.encode(passwordChange.newPassword)
    }
}
