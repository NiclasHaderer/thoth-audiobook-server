package io.thoth.auth.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.thoth.auth.ThothPrincipal
import io.thoth.database.tables.User
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.util.*


internal fun Route.modifyUser() = patch {
    val postModel = call.receive<RegisterUser>()
    val userID = try {
        UUID.fromString(call.request.queryParameters["userID"])
    } catch (e: Exception) {
        serverError(HttpStatusCode.BadRequest, "Could not decode user id")
    }

    val userModel = transaction {
        val user = User.findById(userID) ?: serverError(
            HttpStatusCode.BadRequest,
            "Could not find user with id $userID"
        )
        user.edit = postModel.edit
        user.admin = postModel.admin
        user.username = postModel.username
        user
    }.toModel().toPublicModel()

    call.respond(userModel)
}


internal fun Route.changePassword() = post {
    val passwordChange = call.receive<PasswordChange>()

    if (passwordChange.currentPassword == passwordChange.newPassword) {
        serverError(HttpStatusCode.BadRequest, "New password is the same as the current one")
    }
    val principal = call.principal<ThothPrincipal>()!!

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

    call.respondText("", ContentType.Application.Json)
}
