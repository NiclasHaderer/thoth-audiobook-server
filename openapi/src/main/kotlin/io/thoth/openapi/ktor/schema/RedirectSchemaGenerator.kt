package io.thoth.openapi.ktor.schema

import io.ktor.http.*
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.ktor.responses.RedirectResponse

class RedirectSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*> {
        return StringSchema()
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            RedirectResponse::class,
        )
    }

    override fun generateContentType(classType: ClassType): ContentType {
        return ContentType.Text.Plain
    }
}
