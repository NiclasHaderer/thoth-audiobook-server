package io.thoth.server.database.extensions

import io.thoth.server.di.serialization.Serialization
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.TextColumnType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class JsonColumnType<T : Any>(
    private val mapperClass: KType,
    private val validate: (T) -> Unit = {},
    collate: String? = null,
    eagerLoading: Boolean = false,
) : TextColumnType(collate, eagerLoading), KoinComponent {
    private val serializer by inject<Serialization>()

    override fun valueFromDB(value: Any): Any =
        when (value) {
            is String -> serializer.deserializeValue(value, mapperClass)
            else -> value
        }

    override fun valueToDB(value: Any?): Any? {
        return value?.let(::notNullValueToDB)
    }

    override fun notNullValueToDB(value: Any): Any {
        @Suppress("UNCHECKED_CAST") validate(value as T)
        return serializer.serializeValue(value)
    }
}

inline fun <reified T : Any> Table.json(
    name: String,
    collate: String? = null,
    eagerLoading: Boolean = false,
    noinline validate: (T) -> Unit = {},
): Column<T> {
    return registerColumn(name, JsonColumnType(typeOf<T>(), validate, collate, eagerLoading))
}
