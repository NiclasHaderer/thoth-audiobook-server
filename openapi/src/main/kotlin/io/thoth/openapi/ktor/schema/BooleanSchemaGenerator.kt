package io.thoth.openapi.ktor.schema

import io.ktor.http.ContentType
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.openapi.common.ClassType

class BooleanSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(
        classType: ClassType,
        generateSubType: GenerateSchemaSubtype,
    ): Schema<*> = BooleanSchema()

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(Boolean::class)

    override fun generateContentType(classType: ClassType): ContentType = ContentType.Text.Plain
}
