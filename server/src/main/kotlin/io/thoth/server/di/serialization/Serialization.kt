package io.thoth.server.di.serialization

import java.io.Reader
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface Serialization {
    fun serializeValue(value: Any): String

    fun <T : Any> deserializeValue(
        value: String,
        to: KClass<T>,
    ): T

    fun <T : Any> deserializeValue(
        value: String,
        to: KType,
    ): T

    fun <T : Any> deserializeValue(
        value: Reader,
        to: KClass<T>,
    ): T = deserializeValue(value.readText(), to)

    fun <T : Any> deserializeValue(
        value: Reader,
        to: KType,
    ): T = deserializeValue(value.readText(), to)
}

inline fun <reified T : Any> Serialization.deserializeValue(value: String): T = deserializeValue(value, typeOf<T>())
