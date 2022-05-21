package io.thoth.auth.routes

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.thoth.auth.ThothPrincipal
import io.thoth.common.exceptions.ErrorResponse
import io.thoth.database.tables.User
import org.jetbrains.exposed.sql.transactions.transaction


internal fun Route.userEndpoint() = get {
    val principal = call.principal<ThothPrincipal>()!!
    val userModel = transaction {
        User.getByName(principal.username)?.toPublicModel()
    } ?: throw ErrorResponse(HttpStatusCode.NotFound, "User not found")

    call.respond(userModel)
}
