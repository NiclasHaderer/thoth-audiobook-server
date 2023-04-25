package io.thoth.generators.openapi.schemata

import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.generators.common.ClassType
import io.thoth.generators.openapi.responses.BinaryResponse
import io.thoth.generators.openapi.responses.FileResponse

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
}
