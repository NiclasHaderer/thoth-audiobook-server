package io.thoth.openapi.schema

import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.common.extensions.fields
import io.thoth.common.extensions.optional
import mu.KotlinLogging.logger
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun ClassType.generateSchema(): Map<String, Schema<*>> {
    return (mapOf(clazz.simpleName!! to toSchema(this)))
}

val log = logger {}

internal fun toSchema(classType: ClassType, depth: Int = 0): Schema<*> {
    if (depth > 10) {
        throw Error("Maximum depth exceeded")
    }
    when (classType.clazz) {
        String::class -> return StringSchema()
        Int::class -> return IntegerSchema()
        Long::class -> return IntegerSchema()
        Double::class -> return NumberSchema()
        Float::class -> return NumberSchema()
        Boolean::class -> return BooleanSchema()
        List::class ->
            return ArraySchema().also {
                if (classType.genericArguments.isNotEmpty()) {
                    it.items = toSchema(ClassType.create(classType.genericArguments[0]), depth + 1)
                } else {
                    log.warn { "Could not resolve generic argument for list" }
                }
            }

        Map::class -> return MapSchema().also {
            if (classType.genericArguments.isNotEmpty()) {
                it.items = toSchema(ClassType.create(classType.genericArguments[1]), depth + 1)
            } else {
                log.warn { "Could not resolve generic argument for map" }
            }
        }

        Unit::class -> return StringSchema().maxLength(0)
        Date::class -> return DateTimeSchema()
        LocalDate::class -> return DateSchema()
        LocalDateTime::class -> return DateTimeSchema()
        BigDecimal::class -> return IntegerSchema()
        UUID::class -> return StringSchema()
        else -> {
            val schema = ObjectSchema().also { schema ->
                schema.required = classType.clazz.fields.filter { !it.optional }.map { it.name }
                schema.properties = classType.clazz.fields
                    .associate {
                        it.name to
                            toSchema(
                                classType.fromMember(it),
                                depth + 1,
                            )
                    }
            }
            val debug = Json.mapper().writeValueAsString(schema)
            println(classType.clazz.simpleName!!)
            println(debug)
            return schema
        }
    }
}

data class PaginatedResponse<T, V>(
    val items: List<T>,
    val value: V,
    val total: Long,
)
