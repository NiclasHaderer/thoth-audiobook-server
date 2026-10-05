package io.thoth.server.plugins

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.type.LogicalType
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.thoth.openapi.serializion.jackson.CustomInstantDesSerializer
import io.thoth.openapi.serializion.jackson.CustomInstantSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateDesSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateTimeDesSerializer
import io.thoth.openapi.serializion.jackson.CustomLocalDateTimeSerializer
import io.thoth.openapi.serializion.jackson.OptionalModule
import io.thoth.server.di.serialization.JacksonSerialization
import org.koin.ktor.ext.get
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

fun Application.configureSerialization(): ObjectMapper {
    val serialization = get<JacksonSerialization>()

    install(ContentNegotiation) {
        jackson {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            // A body is read exactly as sent: only an Optional may be left out, a null only goes where the Kotlin
            // type allows one, and nothing is converted into a type it was not sent as.
            disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            coercionConfigFor(LogicalType.Integer).setCoercion(CoercionInputShape.String, CoercionAction.Fail)
            coercionConfigFor(LogicalType.Float).setCoercion(CoercionInputShape.String, CoercionAction.Fail)
            coercionConfigFor(LogicalType.Boolean).apply {
                setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
            }
            coercionConfigFor(LogicalType.Textual).apply {
                setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
            }
            // Ktor registers a default Kotlin module after this block and Jackson ignores that second registration,
            // so this strict one is the one in effect. Without it a null slips into a List<String> unchecked.
            registerModule(kotlinModule { enable(KotlinFeature.NewStrictNullChecks) })
            factory.configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(), true)
            registerModule(Jdk8Module())
            registerModule(OptionalModule())
            registerModule(
                SimpleModule().apply {
                    addSerializer(LocalDateTime::class.java, CustomLocalDateTimeSerializer())
                    addDeserializer(LocalDateTime::class.java, CustomLocalDateTimeDesSerializer())
                    addSerializer(LocalDate::class.java, CustomLocalDateSerializer())
                    addDeserializer(LocalDate::class.java, CustomLocalDateDesSerializer())
                    addSerializer(Instant::class.java, CustomInstantSerializer())
                    addDeserializer(Instant::class.java, CustomInstantDesSerializer())
                },
            )
            serialization.objectMapper = this
        }
    }

    return serialization.objectMapper
}
