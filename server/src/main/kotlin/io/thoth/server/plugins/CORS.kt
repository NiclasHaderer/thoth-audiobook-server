package io.thoth.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.thoth.config.ThothConfig

fun Application.configureCORS(config: ThothConfig) {
    if (config.production) return
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
        allowCredentials = true
        allowHost("localhost:3000", schemes = listOf("http", "https"))
        allowHost("127.0.0.1:3000", schemes = listOf("http", "https"))
    }
}
