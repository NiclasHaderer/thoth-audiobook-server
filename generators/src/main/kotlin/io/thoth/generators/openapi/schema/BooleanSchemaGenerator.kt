package io.thoth.generators.openapi.schema

import io.ktor.http.*
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.generators.common.ClassType

class BooleanSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*> {
        return BooleanSchema()
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Boolean::class)
    }

    override fun generateContentType(classType: ClassType): ContentType {
        return ContentType.Text.Plain
    }
}
