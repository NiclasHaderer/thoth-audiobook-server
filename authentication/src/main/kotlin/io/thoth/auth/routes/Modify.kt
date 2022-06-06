package io.thoth.auth.routes

import io.ktor.http.*
import io.thoth.auth.thothPrincipal
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder


internal fun RouteHandler.modifyUser(userID: IdRoute, editUser: EditUser): UserModel {

    val principal = thothPrincipal()

    return transaction {
        val user = User.findById(userID.id) ?: serverError(
            HttpStatusCode.BadRequest,
            "Could not find user with id $userID"
        )

        if (user.admin && principal.userId != user.id.value) {
            serverError(HttpStatusCode.BadRequest, "A admin user cannot be edited")
        }

        user.edit = editUser.edit
        user.admin = editUser.admin
        user.username = editUser.username

        if (editUser.password != null) {
            val encoder = Argon2PasswordEncoder()
            val encodedPassword = encoder.encode(editUser.password)
            user.passwordHash = encodedPassword
        }

        user
    }.toModel().toPublicModel()
}


internal fun RouteHandler.changePassword(passwordChange: PasswordChange) {
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
