package io.thoth.generators.openapi.schema

import io.ktor.http.*
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.generators.common.ClassType
import java.math.BigDecimal
import java.math.BigInteger

class NumberSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*> {
        return when (classType.clazz) {
            UByte::class,
            Byte::class -> NumberSchema().format("int8")
            UShort::class,
            Short::class -> NumberSchema().format("int16")
            Int::class,
            UInt::class -> NumberSchema().format("int32")
            Long::class,
            ULong::class -> NumberSchema().format("int64")
            BigInteger::class -> NumberSchema().format("int64")
            BigDecimal::class -> NumberSchema().format("decimal")
            Float::class -> NumberSchema().format("float")
            Double::class -> NumberSchema().format("double")
            else -> NumberSchema()
        }
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Int::class,
            Long::class,
            Short::class,
            Byte::class,
            BigDecimal::class,
            BigInteger::class,
            UInt::class,
            ULong::class,
            UShort::class,
            UByte::class,
            Double::class,
            Float::class,
            Number::class,
        )
    }

    override fun generateContentType(classType: ClassType): ContentType {
        return ContentType.Text.Plain
    }
}
