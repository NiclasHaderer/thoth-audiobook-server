package io.thoth.generators.openapi.schemata

import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.generators.common.ClassType

class UnitSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*> {
        return StringSchema().maxLength(0)
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Unit::class,
        )
    }
}
