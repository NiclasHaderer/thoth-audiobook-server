package io.thoth.auth

import io.ktor.auth.*
import io.ktor.routing.*

fun Routing.userAuth(configuration: Routing.() -> Unit) {
    authenticate("user-jwt") {
        this@userAuth.configuration()
    }
}

fun Routing.editUserAuth(configuration: Routing.() -> Unit) {
    authenticate("edit-user-jwt") {
        this@editUserAuth.configuration()
    }
}

fun Routing.adminUserAuth(configuration: Routing.() -> Unit) {
    authenticate("admin-user-jwt") {
        this@adminUserAuth.configuration()
    }
}
