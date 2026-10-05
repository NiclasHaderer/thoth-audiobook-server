package io.thoth.server.plugins

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.thoth.openapi.serializion.jackson.configureForOpenApi
import io.thoth.server.di.serialization.JacksonSerialization
import org.koin.ktor.ext.get

fun Application.configureSerialization(): ObjectMapper {
    val serialization = get<JacksonSerialization>()

    install(ContentNegotiation) {
        jackson {
            configureForOpenApi()
            serialization.objectMapper = this
        }
    }

    return serialization.objectMapper
}
