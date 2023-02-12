package io.thoth.auth.routes

import io.ktor.http.*
import io.thoth.auth.thothPrincipal
import io.thoth.database.access.toModel
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

        val editUserIsAdmin = principal.admin
        val editUserIsSelf = principal.userId == user.id.value

        if (!editUserIsAdmin && !editUserIsSelf) {
            serverError(HttpStatusCode.BadRequest, "You are not allowed to edit this user")
        }

        user.username = editUser.username ?: user.username
        user.edit = editUser.edit ?: user.edit
        user.changePassword = editUser.changePassword ?: user.changePassword

        if (editUserIsAdmin) {
            user.admin = editUser.admin ?: user.admin
            user.enabled = editUser.enabled ?: user.enabled
        }

        if (editUser.password != null) {
            val encoder = Argon2PasswordEncoder()
            val encodedPassword = encoder.encode(editUser.password)
            user.passwordHash = encodedPassword
        }

        user
    }.toModel()
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
