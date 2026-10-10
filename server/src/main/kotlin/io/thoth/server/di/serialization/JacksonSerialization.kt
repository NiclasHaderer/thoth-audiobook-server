package io.thoth.server.di.serialization

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.thoth.openapi.serializion.jackson.configureForOpenApi
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.javaType

class JacksonSerialization : Serialization {
    val objectMapper: ObjectMapper = ObjectMapper().configureForOpenApi()

    override fun serializeValue(value: Any): String = objectMapper.writeValueAsString(value)

    override fun <T : Any> deserializeValue(
        value: String,
        to: KClass<T>,
    ): T = objectMapper.readValue(value, to.java)

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> deserializeValue(
        value: String,
        to: KType,
    ): T =
        objectMapper.readValue(
            value,
            object : TypeReference<T>() {
                override fun getType(): Type = to.javaType
            },
        )
}
