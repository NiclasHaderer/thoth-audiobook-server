package io.thoth.auth.routes

import io.ktor.http.*
import io.thoth.auth.thothPrincipal
import io.thoth.database.access.toModel
import io.thoth.database.tables.User
import io.thoth.models.UserModel
import io.thoth.openapi.RouteHandler
import io.thoth.openapi.serverError
import java.util.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

class ModifyUser(
    var username: String?,
    val password: String?,
    var admin: Boolean?,
    var edit: Boolean?,
    val enabled: Boolean?,
    val changePassword: Boolean?
)

internal fun RouteHandler.modifyUser(userID: UUID, modifyUser: ModifyUser): UserModel {
    val principal = thothPrincipal()

    return transaction {
            val user =
                User.findById(userID) ?: serverError(HttpStatusCode.BadRequest, "Could not find user with id $userID")

            val editUserIsAdmin = principal.admin
            val editUserIsSelf = principal.userId == user.id.value

            if (!editUserIsAdmin && !editUserIsSelf) {
                serverError(HttpStatusCode.BadRequest, "You are not allowed to edit this user")
            }

            user.username = modifyUser.username ?: user.username
            user.edit = modifyUser.edit ?: user.edit
            user.changePassword = modifyUser.changePassword ?: user.changePassword

            if (editUserIsAdmin) {
                user.admin = modifyUser.admin ?: user.admin
                user.enabled = modifyUser.enabled ?: user.enabled
            }

            if (modifyUser.password != null) {
                val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
                val encodedPassword = encoder.encode(modifyUser.password)
                user.passwordHash = encodedPassword
            }

            user
        }
        .toModel()
}
