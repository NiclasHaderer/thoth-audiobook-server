package io.thoth.openapi.ktor.schema

import io.ktor.http.ContentType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.openapi.common.ClassType
import java.util.UUID

class UUIDSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(
        classType: ClassType,
        generateSubType: GenerateSchemaSubtype,
    ): Schema<*> = StringSchema().format("uuid")

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(UUID::class)

    override fun generateContentType(classType: ClassType): ContentType = ContentType.Text.Plain
}
