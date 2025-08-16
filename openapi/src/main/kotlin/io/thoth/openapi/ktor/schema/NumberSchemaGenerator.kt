package io.thoth.openapi.ktor.schema

import io.ktor.http.ContentType
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.openapi.common.ClassType
import java.math.BigDecimal
import java.math.BigInteger

class NumberSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(
        classType: ClassType,
        generateSubType: GenerateSchemaSubtype,
    ): Schema<*> =
        when (classType.clazz) {
            UByte::class,
            Byte::class,
            -> NumberSchema().format("int8")

            UShort::class,
            Short::class,
            -> NumberSchema().format("int16")

            Int::class,
            UInt::class,
            -> NumberSchema().format("int32")

            Long::class,
            ULong::class,
            -> NumberSchema().format("int64")

            BigInteger::class -> NumberSchema().format("int64")
            BigDecimal::class -> NumberSchema().format("decimal")
            Float::class -> NumberSchema().format("float")
            Double::class -> NumberSchema().format("double")
            else -> NumberSchema()
        }

    override fun canGenerate(classType: ClassType): Boolean =
        classType.isSubclassOf(
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

    override fun generateContentType(classType: ClassType): ContentType = ContentType.Text.Plain
}
