package io.github.huiibuh.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.jackson.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson()
    }
}
