package io.thoth.openapi.schema

import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.media.*
import io.thoth.common.extensions.ClassType
import io.thoth.common.extensions.fields
import io.thoth.common.extensions.optional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun generate(classType: ClassType): Map<String, Schema<*>> {
    return mapOf(classType.clazz.simpleName!! to toSchema(classType))
}

fun toSchema(classType: ClassType, depth: Int = 0): Schema<*> {
    if (depth == 0) {
        println("")
    }
    print(classType.clazz.simpleName!!)
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
                val elementType = null
                // it.items =  toSchema(, depth + 1)
            }
        Unit::class -> return StringSchema().maxLength(0)
        Date::class -> return DateTimeSchema()
        LocalDate::class -> return DateSchema()
        LocalDateTime::class -> return DateTimeSchema()
        BigDecimal::class -> return IntegerSchema()
        UUID::class -> return StringSchema()
        else -> {
            val schema = ObjectSchema()
            val properties = classType.clazz.fields
            val required = classType.clazz.fields.filter { !it.optional }.map { it.name }
            schema.required = required
            schema.properties =
                properties.associate {
                    it.name to
                        toSchema(
                            classType.fromMember(it),
                            depth + 1,
                        )
                }
            val debug = Json.mapper().writeValueAsString(schema)
            println(classType.clazz.simpleName!!)
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

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val offset: Long,
    val limit: Int,
)
