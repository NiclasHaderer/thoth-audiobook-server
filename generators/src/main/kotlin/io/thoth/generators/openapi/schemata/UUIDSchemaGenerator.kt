package io.thoth.generators.openapi.schemata

import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.generators.common.ClassType
import java.util.*

class UUIDSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*> {
        return StringSchema()
            .pattern("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
            .minLength(36)
            .maxLength(36)
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            UUID::class,
        )
    }
}
