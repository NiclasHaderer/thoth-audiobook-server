package io.thoth.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.response.header

private val SecurityHeaders =
    createApplicationPlugin("SecurityHeaders") {
        onCall { call -> call.response.header("X-Content-Type-Options", "nosniff") }
    }

fun Application.configureSecurityHeaders() {
    install(SecurityHeaders)
}

fun ApplicationCall.sandbox() {
    response.header("Content-Security-Policy", "default-src 'none'; sandbox")
}
