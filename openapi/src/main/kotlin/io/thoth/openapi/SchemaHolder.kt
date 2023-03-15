package io.thoth.openapi

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.RefUtils
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BinarySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.FileSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.FileResponse
import io.thoth.openapi.responses.RedirectResponse
import io.thoth.openapi.schema.ClassType
import io.thoth.openapi.schema.generateSchema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

object SchemaHolder {
    private var api: OpenAPI? = null
    private val _finalized = AtomicBoolean(false)

    val finalized
        get() = _finalized.get()

    val json: String by lazy {
        if (!finalized) throw Error("Schema has not finalized yet")
        Json.mapper().writeValueAsString(this.api)
    }

    val yaml: String by lazy {
        if (!finalized) throw Error("Schema has not finalized yet")
        Yaml.mapper().writeValueAsString(this.api)
    }

    internal fun lock() {
        this._finalized.set(true)
    }

    internal fun set(api: OpenAPI) {
        if (this.api != null) {
            throw Error("Schema has already been set")
        }
        this.api = api
    }

    fun copy(): OpenAPI {
        val mapper = ObjectMapper()
        val string = mapper.writeValueAsString(api ?: OpenAPI())
        return mapper.readValue(string, OpenAPI::class.java)
    }

    fun addRouteToApi(
        url: String,
        method: HttpMethod,
        requestBody: ClassType,
        params: ClassType,
        responseBody: ClassType,
        responseStatus: HttpStatusCode? = null
    ) {
        val completeResponseStatus: HttpStatusCode =
            responseStatus
                ?: if (responseBody.clazz == Unit::class) {
                    HttpStatusCode.NoContent
                } else if (method == HttpMethod.Post) {
                    HttpStatusCode.Created
                } else {
                    HttpStatusCode.OK
                }

        modify {
            if (paths == null) paths = Paths()

            val pathItem = paths.getOrPut(url) { PathItem() }

            val operation = Operation()
            when (method) {
                HttpMethod.Get -> pathItem.get = operation
                HttpMethod.Head -> pathItem.head = operation
                HttpMethod.Post -> pathItem.post = operation
                HttpMethod.Put -> pathItem.put = operation
                HttpMethod.Delete -> pathItem.delete = operation
                HttpMethod.Options -> pathItem.options = operation
                HttpMethod.Patch -> pathItem.patch = operation
            }

            if (components == null) {
                components = Components()
                components.schemas = mutableMapOf()
            }

            val schemaHolder = components.schemas
            generateSchema(requestBody.clazz)?.also { schemaHolder.putAll(it) }

            operation.requestBody(
                RequestBody()
                    .description("TODO")
                    .content(
                        Content()
                            .addMediaType(
                                "application/json",
                                MediaType()
                                    .schema(
                                        Schema<Any>().also {
                                            it.`$ref` = RefUtils.constructRef(requestBody.clazz.java.simpleName)
                                        },
                                    ),
                            ),
                    ),
            )
            requestBody.generateSchema()
            responseBody.generateSchema()

            generateSchema(responseBody.clazz).also { schemaHolder.putAll(it) }
            operation.responses =
                ApiResponses()
                    .addApiResponse(
                        completeResponseStatus.value.toString(),
                        ApiResponse()
                            .description("TODO")
                            .content(
                                Content()
                                    .addMediaType(
                                        "application/json",
                                        MediaType()
                                            .schema(
                                                Schema<Any>().also {
                                                    it.`$ref` = RefUtils.constructRef(requestBody.clazz.java.simpleName)
                                                },
                                            ),
                                    ),
                            ),
                    )
        }
    }

    private fun generateSchema(clazz: KClass<*>): Map<String, Schema<*>> {
        return getEnumSchema(clazz)
            ?: when (clazz) {
                Unit::class -> mapOf(clazz.java.simpleName to StringSchema().also { it.maxLength = 0 })
                BinaryResponse::class -> mapOf(clazz.java.simpleName to BinarySchema())
                RedirectResponse::class -> mapOf(clazz.java.simpleName to StringSchema())
                FileResponse::class -> mapOf(clazz.java.simpleName to FileSchema())
                String::class -> mapOf(clazz.java.simpleName to StringSchema())
                Boolean::class -> mapOf(clazz.java.simpleName to BooleanSchema())
                java.lang.Boolean::class -> mapOf(clazz.java.simpleName to BooleanSchema())
                Int::class -> mapOf(clazz.java.simpleName to IntegerSchema())
                Integer::class -> mapOf(clazz.java.simpleName to IntegerSchema())
                List::class -> mapOf(clazz.java.simpleName to ArraySchema())
                Long::class -> mapOf(clazz.java.simpleName to IntegerSchema().format("int64"))
                BigDecimal::class -> mapOf(clazz.java.simpleName to IntegerSchema().format(""))
                Date::class -> mapOf(clazz.java.simpleName to DateSchema())
                LocalDate::class -> mapOf(clazz.java.simpleName to DateSchema())
                LocalDateTime::class -> mapOf(clazz.java.simpleName to DateTimeSchema())
                else -> ModelConverters.getInstance().read(clazz.java)
            }
    }

    private fun getEnumSchema(clazz: KClass<*>): Map<String, Schema<*>>? {
        val values = clazz.java.enumConstants ?: return null

        val schema = StringSchema()
        for (enumVal in values) {
            schema.addEnumItem(enumVal.toString())
        }
        return mapOf(clazz.java.simpleName to schema)
    }

    fun modify(configure: OpenAPI.() -> Unit) {
        if (this.api == null) {
            throw Error("Schema has not been set")
        }
        if (finalized) {
            throw Error("Application startup has completed. You cannot modify the API object any more")
        }

        synchronized(this.api!!) { this.api!!.configure() }
    }
}
