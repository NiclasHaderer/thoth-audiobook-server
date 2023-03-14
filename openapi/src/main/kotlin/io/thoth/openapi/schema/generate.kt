package io.thoth.openapi.schema

import com.google.gson.reflect.TypeToken
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.models.BookModel
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.javaType

private val <T, V> KProperty1<T, V>.optional: Boolean
    get() = returnType.isMarkedNullable

val KClass<*>.schema: Schema<*>
    get() = toSchema(this, object : TypeToken<Any>() {}.type)

val KClass<*>.fields: List<KProperty1<out Any, *>>
    get() = declaredMemberProperties.toList()

fun generate(clazz: KClass<*>, type: Type): Map<String, Schema<*>> {
    return mapOf(clazz.java.simpleName to toSchema(clazz, type))
}

@OptIn(ExperimentalStdlibApi::class)
fun toSchema(clazz: KClass<*>, type: Type, depth: Int = 0): Schema<*> {
    if (depth == 0) {
        println("")
    }
    print(clazz.java.simpleName)
    if (depth > 10) {
        throw Error("Maximum depth exceeded")
    }
    when (clazz) {
        String::class -> return StringSchema()
        Int::class -> return IntegerSchema()
        Long::class -> return IntegerSchema()
        Double::class -> return NumberSchema()
        Float::class -> return NumberSchema()
        Boolean::class -> return BooleanSchema()
        List::class ->
            return ArraySchema().also {
                val elementType =
                    try {
                        getListType(type)
                    } catch (e: Exception) {
                        println("asdf")
                        throw e
                    }
                //            it.items = toSchema(
                //                elementType,
                //                elementType.starProjectedType.javaType,
                //                depth + 1,
                //            )
            }
        Unit::class -> return StringSchema().maxLength(0)
        Date::class -> return DateTimeSchema()
        LocalDate::class -> return DateSchema()
        LocalDateTime::class -> return DateTimeSchema()
        BigDecimal::class -> return IntegerSchema()
        UUID::class -> return StringSchema()
        else -> {
            val schema = ObjectSchema()
            val properties = clazz.fields
            val required = clazz.fields.filter { !it.optional }.map { it.name }
            schema.required = required
            schema.properties =
                properties.associate {
                    it.name to
                        toSchema(
                            it.returnType.classifier as KClass<*>,
                            it.returnType.javaType,
                            depth + 1,
                        )
                }
            val debug = Json.mapper().writeValueAsString(schema)
            println(clazz.java.simpleName)
            println(debug)
            return schema
        }
    }
}

// fun getListType(type: Type): KClass<*> {
//    if (type !is ParameterizedType) {
//        throw IllegalArgumentException("List type not parameterized")
//    }
//    val elementType = type.actualTypeArguments.first()
//    if (elementType is WildcardType) {
//        return (elementType.upperBounds.first() as Class<*>).kotlin
//    } else if (elementType !is Class<*>) {
//        throw IllegalArgumentException("List element type not a class")
//    }
//    return elementType.kotlin
// }

fun getListType(type: Type): Class<*> {
    if (type !is ParameterizedType) {
        throw IllegalArgumentException("List type not parameterized")
    }
    val elementType = type.actualTypeArguments.first()
    return when (elementType) {
        is Class<*> -> elementType
        is WildcardType -> elementType.upperBounds.first() as Class<*>
        else -> Any::class.java
    }
}

inline fun <T> asdf(): TypeToken<T> {
    return object : TypeToken<T>() {}
}

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val offset: Long,
    val limit: Int,
)

fun main() {
    val objectType = object : TypeToken<PaginatedResponse<BookModel>>() {}.type
    val listType = object : TypeToken<List<BookModel>>() {}.type

    getListType(objectType).also { println(it) }
    getListType(listType).also { println(it) }
}
