package io.thoth.openapi.schema

import io.swagger.v3.core.util.RefUtils
import io.swagger.v3.oas.models.media.*
import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.FileResponse
import io.thoth.openapi.responses.RedirectResponse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
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
            RedirectResponse::class -> return null to StringSchema()
            BinaryResponse::class -> return null to StringSchema().format("binary")
            FileResponse::class -> return null to StringSchema().format("binary")
            ByteArray::class -> return null to StringSchema().format("binary")
            String::class -> return null to StringSchema()
            Int::class -> return null to IntegerSchema()
            Long::class -> return null to IntegerSchema()
            Double::class -> return null to NumberSchema()
            Float::class -> return null to NumberSchema()
            Boolean::class -> return null to BooleanSchema()
            ULong::class -> return null to IntegerSchema()
            Unit::class -> return null to StringSchema().maxLength(0)
            Date::class -> return null to DateTimeSchema()
            LocalDate::class -> return null to DateSchema()
            LocalDateTime::class -> return null to DateTimeSchema()
            BigDecimal::class -> return null to IntegerSchema()
            UUID::class ->
                return "UUID".takeIf { embedSchemas } to
                    StringSchema().also {
                        it.pattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
                        it.minLength = 36
                        it.maxLength = 36
                    }
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
                    MapSchema().also {
                        if (classType.genericArguments.size != 2) {
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
                            it.items = schema
                        } else {
                            log.warn { "Could not resolve generic argument for map" }
                        }
                    }
            else -> {
                val objectSchema = ObjectSchema()
                objectSchema.required =
                    classType.clazz.declaredMemberProperties
                        .filter { it.visibility == KVisibility.PUBLIC }
                        .filter { !it.returnType.isMarkedNullable }
                        .map { it.name }
                objectSchema.properties =
                    classType.clazz.declaredMemberProperties
                        .filter { it.visibility == KVisibility.PUBLIC }
                        .associate {
                            var (schemaName, schema) =
                                createSchemaForClassType(
                                    classType.fromMember(it),
                                    embedSchemas,
                                    namedSideSchemas,
                                )
                            if (schemaName != null) {
                                namedSideSchemas[schemaName] = schema
                                schema = createRef(schemaName)
                            }

                            it.name to schema
                        }
                return classType.clazz.qualifiedName.takeIf { embedSchemas } to objectSchema
            }
        }
    }
}
