package io.thoth.server.plugins

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.thoth.openapi.serializion.jackson.CustomLocalDateDesSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateTimeDesSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateTimeSerializer
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.di.serialization.Serialization
import java.time.LocalDate
import java.time.LocalDateTime
import org.koin.ktor.ext.inject

fun Application.configureSerialization() {
    @Suppress("UNCHECKED_CAST") val serialization by inject<Serialization>() as Lazy<JacksonSerialization>

    install(ContentNegotiation) {
        jackson {
            val module = SimpleModule()
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
            module.addSerializer(LocalDateTime::class.java, CustomLocalDateTimeSerializer())
            module.addDeserializer(LocalDateTime::class.java, CustomLocalDateTimeDesSerializer())
            module.addSerializer(LocalDate::class.java, CustomLocalDateSerializer())
            module.addDeserializer(LocalDate::class.java, CustomLocalDateDesSerializer())
            registerModule(module)
            serialization.objectMapper = this
        }
    }
}
