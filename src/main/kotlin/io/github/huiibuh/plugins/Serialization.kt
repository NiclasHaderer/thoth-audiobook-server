package io.github.huiibuh.plugins

import com.fasterxml.jackson.databind.module.SimpleModule
import io.github.huiibuh.serializers.jackson.CustomLocalDateTimeDesSerializer
import io.github.huiibuh.serializers.jackson.CustomLocalDateTimeSerializer
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import java.time.LocalDateTime


fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            val module = SimpleModule()
            module.addSerializer(LocalDateTime::class.java, CustomLocalDateTimeSerializer())
            module.addDeserializer(LocalDateTime::class.java, CustomLocalDateTimeDesSerializer())
            registerModule(module)
        }
    }
}
