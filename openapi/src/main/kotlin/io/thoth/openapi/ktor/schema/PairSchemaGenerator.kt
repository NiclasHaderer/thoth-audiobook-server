package io.thoth.openapi.ktor.schema

import io.ktor.http.*
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.openapi.common.ClassType

class PairSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*> {
        return ArraySchema().also { schema ->
            val subSchema = classType.genericArguments.map(generateSubType)
            schema.prefixItems = subSchema.map { it.reference() }
        }
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Pair::class)
    }

    override fun generateContentType(classType: ClassType): ContentType {
        return ContentType.Application.Json
    }
}
