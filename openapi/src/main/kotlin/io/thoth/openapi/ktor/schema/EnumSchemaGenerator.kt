package io.thoth.openapi.ktor.schema

import io.ktor.http.ContentType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.openapi.common.ClassType

class EnumSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(
        classType: ClassType,
        generateSubType: GenerateSchemaSubtype,
    ): Schema<*> {
        val values = classType.enumValues()
        if (values == null || values.isEmpty()) {
            log.warn { "Enum ${classType.simpleName} has no values" }
            return StringSchema()
        }
        return StringSchema().also { schema -> values.forEach { schema.addEnumItemObject(it.toString()) } }
    }

    override fun generateName(
        classType: ClassType,
        generateSubType: GenerateSchemaSubtype,
    ): String = classType.simpleName

    override fun canGenerate(classType: ClassType): Boolean = classType.isEnum()

    override fun generateContentType(classType: ClassType): ContentType = ContentType.Text.Plain
}
