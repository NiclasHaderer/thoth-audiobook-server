package io.thoth.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.thoth.openapi.serverError

internal enum class GuardTypes(val value: String) {
    User("user-jwt"),
    EditUser("edit-user-jwt"),
    AdminUser("admin-user-jwt"),
}

fun Route.userAuth(config: Route.() -> Unit) = authenticate(GuardTypes.User.value) { this.config() }

fun Route.editUserAuth(config: Route.() -> Unit) = authenticate(GuardTypes.EditUser.value) { this.config() }

fun Route.adminUserAuth(config: Route.() -> Unit) = authenticate(GuardTypes.AdminUser.value) { this.config() }

fun PipelineContext<Unit, ApplicationCall>.thothPrincipal(): ThothPrincipal {
    return call.principal()
        ?: serverError(HttpStatusCode.InternalServerError, "Call has to be surrounded with a jwt login")
}

fun PipelineContext<Unit, ApplicationCall>.thothPrincipalOrNull(): ThothPrincipal? = call.principal()
