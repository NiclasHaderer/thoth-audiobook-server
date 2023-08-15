package io.thoth.server.di.serialization

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.javaType

class JacksonSerialization : Serialization {
    var objectMapper: ObjectMapper? = null

    override fun serializeValue(value: Any): String {
        require(objectMapper != null) { "ObjectMapper not initialized" }
        return objectMapper!!.writeValueAsString(value)
    }

    override fun <T : Any> deserializeValue(value: String, to: KClass<T>): T {
        return runDeserialization(value, to.java)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> deserializeValue(value: String, to: KType): T {
        return runDeserialization(
            value,
            object : TypeReference<T>() {
                override fun getType(): Type {
                    return to.javaType
                }
            },
        )
    }

    private fun <T : Any> runDeserialization(value: String, to: Class<T>): T {
        require(objectMapper != null) { "ObjectMapper not initialized" }
        return objectMapper!!.readValue(value, to)
    }

    private fun <T : Any> runDeserialization(value: String, to: TypeReference<T>): T {
        require(objectMapper != null) { "ObjectMapper not initialized" }
        return objectMapper!!.readValue(value, to)
    }
}
