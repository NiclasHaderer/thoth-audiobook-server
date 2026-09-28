package io.thoth.openapi.ktor.schema

import io.ktor.http.ContentType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.openapi.common.ClassType
import java.util.Optional

class OptionalSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(
        classType: ClassType,
        generateSubType: GenerateSchemaSubtype,
    ): Schema<*> {
        if (classType.genericArguments.isEmpty()) {
            log.warn { "Could not resolve generic argument for optional" }
            return ObjectSchema()
        }
        return generateSubType(classType.genericArguments[0]).reference()
    }

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(Optional::class)

    override fun generateContentType(classType: ClassType): ContentType = ContentType.Application.Json
}
