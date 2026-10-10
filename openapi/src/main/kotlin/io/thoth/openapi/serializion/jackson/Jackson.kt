package io.thoth.openapi.serializion.jackson

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.type.LogicalType
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Makes the mapper read and write bodies the way the generated schema and clients describe them: only a Patch may be
 * left out, a null only goes where the Kotlin type allows one, and nothing is converted into a type it was not sent as.
 */
fun ObjectMapper.configureForOpenApi(): ObjectMapper {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
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
    // Jackson ignores a second registration of the Kotlin module, so this strict one has to come before any default
    // one, such as the one Ktor adds after its configuration block. Without it a null slips into a List<String>.
    registerModule(kotlinModule { enable(KotlinFeature.NewStrictNullChecks) })
    factory.configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(), true)
    registerModule(PatchModule())
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
    return this
}
