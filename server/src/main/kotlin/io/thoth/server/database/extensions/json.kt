package io.thoth.server.database.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.reflect.KClass
import org.h2.jdbc.JdbcClob
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.TextColumnType

class JsonColumnType<T : Any>(
    private val mapperClass: KClass<T>,
    private val validate: (T) -> Unit = {},
    collate: String? = null,
    eagerLoading: Boolean = false,
) : TextColumnType(collate, eagerLoading) {
    companion object {
        private val mapper by lazy { ObjectMapper().registerKotlinModule() }
    }

    override fun valueFromDB(value: Any): Any =
        when (value) {
            is String -> mapper.readValue(value, mapperClass.java)
            is JdbcClob -> mapper.readValue(value.characterStream, mapperClass.java)
            else -> value
        }

    override fun valueToDB(value: Any?): Any? {
        return value?.let(::notNullValueToDB)
    }

    override fun notNullValueToDB(value: Any): Any {
        @Suppress("UNCHECKED_CAST") validate(value as T)
        return mapper.writeValueAsString(value)
    }
}

inline fun <reified T : Any> Table.json(
    name: String,
    collate: String? = null,
    eagerLoading: Boolean = false,
    noinline validate: (T) -> Unit = {},
): Column<T> {
    return registerColumn(name, JsonColumnType(T::class, validate, collate, eagerLoading))
}
