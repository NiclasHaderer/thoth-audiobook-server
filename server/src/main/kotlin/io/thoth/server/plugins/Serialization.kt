package io.thoth.server.plugins

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.thoth.openapi.serializion.jackson.CustomLocalDateDesSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateTimeDesSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateTimeSerializer
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.di.serialization.Serialization
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.LocalDateTime

fun Application.configureSerialization(): ObjectMapper {
    @Suppress("UNCHECKED_CAST")
    val serialization by inject<Serialization>() as Lazy<JacksonSerialization>

    install(ContentNegotiation) {
        jackson {
            this
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)

            factory.configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(), true)

            jacksonMapperBuilder().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)

            val module =
                SimpleModule().apply {
                    addSerializer(LocalDateTime::class.java, CustomLocalDateTimeSerializer())
                    addDeserializer(LocalDateTime::class.java, CustomLocalDateTimeDesSerializer())
                    addSerializer(LocalDate::class.java, CustomLocalDateSerializer())
                    addDeserializer(LocalDate::class.java, CustomLocalDateDesSerializer())
                }
            registerModule(module)
            serialization.objectMapper = this
        }
    }
    assert(serialization.objectMapper != null) { "ObjectMapper is not initialized in ContentNegotiation" }
    return serialization.objectMapper!!
}
