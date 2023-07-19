package io.thoth.openapi.openapi.schema

import io.ktor.http.*
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.openapi.responses.BinaryResponse
import io.thoth.openapi.openapi.responses.FileResponse

class FileSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*> {
        return StringSchema().format("binary")
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            FileResponse::class,
            BinaryResponse::class,
            ByteArray::class,
        )
    }

    override fun generateContentType(classType: ClassType): ContentType {
        return ContentType.Application.OctetStream
    }
}
