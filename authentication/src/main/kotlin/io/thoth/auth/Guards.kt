package io.thoth.auth

import io.ktor.server.auth.*
import io.ktor.server.routing.*


internal enum class GuardTypes(val value: String) {
    User("user-jwt"),
    EditUser("edit-user-jwt"),
    AdminUser("admin-user-jwt"),
}


fun Route.userAuth(config: Route.() -> Unit) = authenticate(GuardTypes.User.value) {
    this.config()
}

fun Route.editUserAuth(config: Route.() -> Unit) = authenticate(GuardTypes.EditUser.value) {
    this.config()
}

fun Route.adminUserAuth(config: Route.() -> Unit) = authenticate(GuardTypes.AdminUser.value) {
    this.config()
}
