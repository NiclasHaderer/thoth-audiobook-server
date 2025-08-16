package io.thoth.openapi.ktor.schema

import io.ktor.http.ContentType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.openapi.common.ClassType

class UnitSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(
        classType: ClassType,
        generateSubType: GenerateSchemaSubtype,
    ): Schema<*> = StringSchema().maxLength(0)

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(Unit::class)

    override fun generateContentType(classType: ClassType): ContentType = ContentType.Text.Plain
}
