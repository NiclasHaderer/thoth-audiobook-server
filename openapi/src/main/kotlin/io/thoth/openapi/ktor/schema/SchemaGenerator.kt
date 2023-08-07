package io.thoth.openapi.ktor.schema

import io.ktor.http.*
import io.swagger.v3.core.util.RefUtils
import io.swagger.v3.oas.models.media.Schema
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging.logger

typealias GenerateSchemaSubtype = (ClassType) -> SchemaGenerator.WrappedSchema

abstract class SchemaGenerator {
    protected val log = logger {}

    class WrappedSchema(val schema: Schema<*>, val name: String?, val contentType: ContentType) {
        fun reference(): Schema<*> {
            return if (name != null) {
                Schema<Any>()
                    .`$ref`(
                        RefUtils.constructRef(
                            name,
                        ),
                    )
            } else {
                schema
            }
        }
    }

    fun createSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): WrappedSchema {
        return WrappedSchema(
            generateSchema(classType, generateSubType),
            generateName(classType, generateSubType),
            generateContentType(classType),
        )
    }

    abstract fun generateSchema(classType: ClassType, generateSubType: GenerateSchemaSubtype): Schema<*>

    open fun generateName(classType: ClassType, generateSubType: GenerateSchemaSubtype): String? = null

    abstract fun canGenerate(classType: ClassType): Boolean

    abstract fun generateContentType(classType: ClassType): ContentType

    open fun priority(classType: ClassType): Int = 0
}

val schemaGenerators: List<SchemaGenerator> =
    listOf(
        DateSchemaGenerator(),
        FileSchemaGenerator(),
        ListSchemaGenerator(),
        MapSchemaGenerator(),
        NumberSchemaGenerator(),
        ObjectSchemaGenerator(),
        PairSchemaGenerator(),
        RedirectSchemaGenerator(),
        StringSchemaGenerator(),
        UnitSchemaGenerator(),
        UUIDSchemaGenerator(),
        BooleanSchemaGenerator()
    )

fun Pair<SchemaGenerator.WrappedSchema, List<SchemaGenerator.WrappedSchema>>.toNamed():
    Pair<SchemaGenerator.WrappedSchema, Map<String, Schema<*>>> {
    val namedSchemas = this.second.filterNot { it.name == null }.associate { it.name as String to it.schema }
    val firstAsNamed =
        if (this.first.name == null) {
            mapOf()
        } else {
            mapOf(this.first.name as String to this.first.schema)
        }
    val finalNamed = firstAsNamed + namedSchemas

    return this.first to finalNamed
}

fun generateSchemas(classType: ClassType): Pair<SchemaGenerator.WrappedSchema, List<SchemaGenerator.WrappedSchema>> {
    val generator = schemaGenerators.filter { it.canGenerate(classType) }.maxBy { it.priority(classType) }
    val generatedSubTypes = mutableListOf<SchemaGenerator.WrappedSchema>()
    val type =
        generator.createSchema(classType) { subType ->
            val (actual, generatedSubType) = generateSchemas(subType)
            generatedSubTypes.addAll(generatedSubType)
            actual
        }
    generatedSubTypes.add(type)
    return type to generatedSubTypes
}
