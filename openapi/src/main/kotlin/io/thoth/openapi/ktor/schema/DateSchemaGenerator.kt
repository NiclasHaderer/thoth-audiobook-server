package io.thoth.openapi.ktor.schema

import io.ktor.http.*
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.openapi.common.ClassType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DateSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(
        classType: ClassType,
        generateSubType: GenerateSchemaSubtype,
    ): Schema<*> =
        when (classType.clazz) {
            Date::class -> DateTimeSchema()
            LocalDate::class -> DateSchema()
            LocalDateTime::class -> DateTimeSchema()
            else -> NumberSchema()
        }

    override fun canGenerate(classType: ClassType): Boolean =
        classType.isSubclassOf(Date::class, LocalDate::class, LocalDateTime::class)

    override fun generateContentType(classType: ClassType): ContentType = ContentType.Text.Plain
}
