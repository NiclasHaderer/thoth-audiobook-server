package io.thoth.generators.openapi.schemata

import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.generators.common.ClassType

class ListSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*> {
        return ArraySchema().also {
            if (classType.genericArguments.isNotEmpty()) {
                val subSchema = generateSubType(classType.genericArguments[0])
                it.items = subSchema.reference()
            } else {
                log.warn { "Could not resolve generic argument for list" }
                it.items = ObjectSchema()
            }
        }
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            List::class,
            MutableList::class,
            ArrayList::class,
            Array::class,
            Collection::class,
            HashSet::class,
            LinkedHashSet::class,
            Set::class,
        )
    }
}
