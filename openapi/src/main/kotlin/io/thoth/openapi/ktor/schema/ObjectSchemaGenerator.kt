package io.thoth.openapi.ktor.schema

import io.ktor.http.*
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.nullable
import kotlin.reflect.KVisibility

class ObjectSchemaGenerator : SchemaGenerator() {
    override fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*> {
        val superClasses =
            classType.superClasses
                .filter { it.memberProperties.isNotEmpty() }
                .filterNot { it.clazz == Enum::class }
                .map(generateSubType)

        return ObjectSchema().also { schema ->
            schema.required =
                classType.properties
                    .filter { it.visibility == KVisibility.PUBLIC }
                    .filter { !it.nullable }
                    .map { it.name }
            schema.properties =
                classType.properties
                    .filter { it.visibility == KVisibility.PUBLIC }
                    .associate {
                        val subSchema = generateSubType(classType.forMember(it))
                        it.name to subSchema.reference()
                    }
            schema.allOf = superClasses.map { it.reference() }
        }
    }

    override fun generateName(classType: ClassType, generateSubType: GenerateSchemaSubtype): String {
        var schemaName = classType.simpleName
        if (classType.genericArguments.isNotEmpty()) {
            schemaName += classType.genericArguments.joinToString(prefix = "<", postfix = ">") { it.simpleName }
        }
        return schemaName
    }

    override fun generateContentType(classType: ClassType): ContentType {
        return ContentType.Application.Json
    }

    override fun canGenerate(classType: ClassType): Boolean = true
    override fun priority(classType: ClassType): Int = -1
}
