package io.thoth.server.plugins

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.thoth.openapi.serializion.jackson.CustomLocalDateDesSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateTimeDesSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateTimeSerializer
import io.thoth.server.di.serialization.JacksonSerialization
import org.koin.ktor.ext.get
import java.time.LocalDate
import java.time.LocalDateTime

fun Application.configureSerialization(): ObjectMapper {
    val serialization = get<JacksonSerialization>()

    install(ContentNegotiation) {
        jackson {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            factory.configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(), true)
            registerModule(
                SimpleModule().apply {
                    addSerializer(LocalDateTime::class.java, CustomLocalDateTimeSerializer())
                    addDeserializer(LocalDateTime::class.java, CustomLocalDateTimeDesSerializer())
                    addSerializer(LocalDate::class.java, CustomLocalDateSerializer())
                    addDeserializer(LocalDate::class.java, CustomLocalDateDesSerializer())
                },
            )
            serialization.objectMapper = this
        }
    }

    return serialization.objectMapper
}
