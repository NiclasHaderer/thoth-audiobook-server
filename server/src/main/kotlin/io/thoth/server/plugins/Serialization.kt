package io.thoth.server.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json


fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true

        })

//        jackson {
//            val module = SimpleModule()
//            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//            enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
//            module.addSerializer(LocalDateTime::class.java, CustomLocalDateTimeSerializer())
//            module.addDeserializer(LocalDateTime::class.java, CustomLocalDateTimeDesSerializer())
//            module.addSerializer(LocalDate::class.java, CustomLocalDateSerializer())
//            module.addDeserializer(LocalDate::class.java, CustomLocalDateDesSerializer())
//            registerModule(module)
//        }
    }
}
