package io.thoth.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.cors.CORS

fun Application.configureCORS() {
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Patch)
        method(HttpMethod.Delete)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.ContentType)
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

}
