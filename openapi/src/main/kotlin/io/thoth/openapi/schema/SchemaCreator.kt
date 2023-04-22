package io.thoth.openapi.schema

import io.swagger.v3.core.util.RefUtils
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.thoth.openapi.nullable
import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.FileResponse
import io.thoth.openapi.responses.RedirectResponse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KVisibility
import mu.KotlinLogging.logger

fun ClassType.generateSchema(embedSchemas: Boolean = true): Pair<Schema<*>, Map<String, Schema<*>>> {
    val namedSchemas = mutableMapOf<String, Schema<*>>()
    var (schemaName, schema) = SchemaCreator.createSchemaForClassType(this, embedSchemas, namedSchemas)
    if (schemaName != null) {
        namedSchemas[schemaName] = schema
        schema = SchemaCreator.createRef(schemaName)
    }
    return schema to namedSchemas
}

typealias SchemaName = String?

private object SchemaCreator {
    private val log = logger {}
    fun createRef(name: String): Schema<*> {
        return Schema<Any>()
            .`$ref`(
                RefUtils.constructRef(
                    name,
                ),
            )
    }

    fun createSchemaForClassType(
        classType: ClassType,
        embedSchemas: Boolean,
        namedSideSchemas: MutableMap<String, Schema<*>>
    ): Pair<SchemaName, Schema<*>> {
        if (classType.isEnum) {
            val schema = StringSchema()
            val values = classType.clazz.java.enumConstants
            for (enumVal in values) {
                schema.addEnumItem(enumVal.toString())
            }
            return classType.clazz.qualifiedName.takeIf { embedSchemas } to schema
        }

        when (classType.clazz) {
            // Custom responses
            RedirectResponse::class -> return null to StringSchema()
            BinaryResponse::class -> return null to StringSchema().format("binary")
            FileResponse::class -> return null to StringSchema().format("binary")
            // Basic types
            ByteArray::class -> return null to StringSchema().format("binary")
            Unit::class -> return null to StringSchema().maxLength(0)
            String::class -> return null to StringSchema()
            Any::class -> return null to ObjectSchema()
            // Numbers
            Int::class -> return null to IntegerSchema().format("int32")
            Long::class -> return null to IntegerSchema().format("int64")
            Double::class -> return null to NumberSchema().format("double")
            Float::class -> return null to NumberSchema().format("float")
            Boolean::class -> return null to BooleanSchema()
            ULong::class -> return null to IntegerSchema().format("int64").minimum(BigDecimal(0))
            BigDecimal::class -> return null to IntegerSchema().format("int64")
            // Date
            Date::class -> return null to DateTimeSchema()
            LocalDate::class -> return null to DateSchema()
            LocalDateTime::class -> return null to DateTimeSchema()
            // Complex types
            UUID::class ->
                return classType.clazz.qualifiedName.takeIf { embedSchemas } to
                    StringSchema()
                        .pattern("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
                        .minLength(36)
                        .maxLength(36)
            List::class ->
                return null to
                    ArraySchema().also {
                        if (classType.genericArguments.isNotEmpty()) {
                            var (schemaName, schema) =
                                createSchemaForClassType(
                                    classType.genericArguments[0],
                                    embedSchemas,
                                    namedSideSchemas,
                                )
                            if (schemaName != null) {
                                namedSideSchemas[schemaName] = schema
                                schema = createRef(schemaName)
                            }
                            it.items = schema
                        } else {
                            log.warn { "Could not resolve generic argument for list" }
                        }
                    }
            Map::class ->
                return null to
                    ObjectSchema().also {
                        if (classType.genericArguments.size == 2) {
                            var (schemaName, schema) =
                                createSchemaForClassType(
                                    classType.genericArguments[1],
                                    embedSchemas,
                                    namedSideSchemas,
                                )
                            if (schemaName != null) {
                                namedSideSchemas[schemaName] = schema
                                schema = createRef(schemaName)
                            }
                            it.additionalProperties = schema
                        } else {
                            log.warn { "Could not resolve generic argument for map" }
                        }
                    }
            else -> {
                val objectSchema =
                    ObjectSchema().also { schema ->
                        schema.required =
                            classType.properties
                                .filter { it.visibility == KVisibility.PUBLIC }
                                .filter { !it.nullable }
                                .map { it.name }
                        schema.properties =
                            classType.properties
                                .filter { it.visibility == KVisibility.PUBLIC }
                                .associate {
                                    var (schemaName, propSchema) =
                                        createSchemaForClassType(
                                            classType.fromMember(it),
                                            embedSchemas,
                                            namedSideSchemas,
                                        )
                                    if (schemaName != null) {
                                        namedSideSchemas[schemaName] = propSchema
                                        propSchema = createRef(schemaName)
                                    }

                                    it.name to propSchema
                                }
                    }

                var schemaName = classType.clazz.qualifiedName
                if (classType.genericArguments.isNotEmpty()) {
                    schemaName +=
                        classType.genericArguments.joinToString(prefix = "<", postfix = ">") {
                            it.clazz.qualifiedName!!
                        }
                }

                return schemaName.takeIf { embedSchemas } to objectSchema
            }
        }
    }
}
