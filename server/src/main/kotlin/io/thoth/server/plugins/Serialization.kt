package io.thoth.server.plugins

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.thoth.common.serializion.jackson.CustomLocalDateDesSerializer
import io.thoth.common.serializion.jackson.CustomLocalDateSerializer
import io.thoth.common.serializion.jackson.CustomLocalDateTimeDesSerializer
import io.thoth.common.serializion.jackson.CustomLocalDateTimeSerializer
import java.time.LocalDate
import java.time.LocalDateTime

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            val module = SimpleModule()
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            module.addSerializer(LocalDateTime::class.java, CustomLocalDateTimeSerializer())
            module.addDeserializer(LocalDateTime::class.java, CustomLocalDateTimeDesSerializer())
            module.addSerializer(LocalDate::class.java, CustomLocalDateSerializer())
            module.addDeserializer(LocalDate::class.java, CustomLocalDateDesSerializer())
            registerModule(module)
        }
    }
}
