package io.thoth.openapi.ktor.schema

import io.ktor.http.ContentType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.acceptsNull
import io.thoth.openapi.common.isPatch
import kotlin.reflect.KVisibility

class ObjectSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(
        classType: ClassType,
        generateSubType: GenerateSchemaSubtype,
    ): Schema<*> {
        val superClasses =
            classType.superClasses
                .filter { it.memberProperties.isNotEmpty() }
                .filterNot { it.clazz == Enum::class }
                .map(generateSubType)

        return ObjectSchema().also { schema ->
            schema.required =
                classType.properties
                    .filter { it.visibility == KVisibility.PUBLIC }
                    .filter { !it.isPatch }
                    .map { it.name }
            schema.properties =
                classType.properties
                    .filter { it.visibility == KVisibility.PUBLIC }
                    .associate {
                        val subSchema = generateSubType(classType.forMember(it))
                        it.name to if (it.acceptsNull) nullable(subSchema) else subSchema.reference()
                    }
            if (superClasses.isNotEmpty()) {
                schema.allOf = superClasses.map { it.reference() }
            }
        }
    }

    // OpenAPI 3.0 ignores every sibling of a $ref, so a named schema has to be wrapped to carry the nullable
    private fun nullable(schema: WrappedSchema): Schema<*> {
        val reference = schema.reference()
        val wrapped = if (reference.`$ref` != null) Schema<Any>().allOf(listOf(reference)) else reference
        return wrapped.nullable(true)
    }

    override fun generateName(
        classType: ClassType,
        generateSubType: GenerateSchemaSubtype,
    ): String {
        var schemaName = classType.simpleName
        if (classType.genericArguments.isNotEmpty()) {
            schemaName += classType.genericArguments.joinToString(separator = "_", prefix = "_") { it.simpleName }
        }
        return schemaName
    }

    override fun generateContentType(classType: ClassType): ContentType = ContentType.Application.Json

    override fun canGenerate(classType: ClassType): Boolean = true

    override fun priority(classType: ClassType): Int = -1
}
