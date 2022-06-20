package io.thoth.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*

fun Application.configureCORS() {
    install(io.ktor.server.plugins.cors.routing.CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

}
