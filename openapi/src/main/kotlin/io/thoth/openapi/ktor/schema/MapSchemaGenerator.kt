package io.thoth.openapi.ktor.schema

import io.ktor.http.*
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.openapi.common.ClassType

class MapSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*> {
        return ObjectSchema().also {
            if (classType.genericArguments.size == 2) {
                val subSchema = generateSubType(classType.genericArguments[1])
                it.additionalProperties = subSchema
            } else {
                log.warn { "Could not resolve generic argument for map" }
            }
        }
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Map::class, HashMap::class, LinkedHashMap::class)
    }

    override fun generateContentType(classType: ClassType): ContentType {
        return ContentType.Application.Json
    }
}
