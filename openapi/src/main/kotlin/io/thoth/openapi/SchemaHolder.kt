package io.thoth.openapi

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
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
        requestBody: KClass<*>,
        params: KClass<*>,
        responseBody: KClass<*>,
        responseStatus: HttpStatusCode
    ) {
        modify {
            if (paths == null) this.paths = Paths()

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

            operation.requestBody(
                RequestBody().description("TODO").content(
                    Content().addMediaType("application/json", MediaType().schema(generateSchema(requestBody)))
                )
            )

            operation.responses = ApiResponses().addApiResponse(
                responseStatus.value.toString(),
                ApiResponse()
                    .description("TODO")
                    .content(
                        Content().addMediaType(
                            "application/json",
                            MediaType().schema(generateSchema(responseBody))
                        )
                    )
            )
        }
    }

    private fun generateSchema(clazz: KClass<*>): Schema<*>? {
        return getEnumSchema(clazz) ?: when (clazz) {
            String::class -> StringSchema()
            Boolean::class -> BooleanSchema()
            java.lang.Boolean::class -> BooleanSchema()
            Int::class -> IntegerSchema()
            Integer::class -> IntegerSchema()
            List::class -> ArraySchema()
            Long::class -> IntegerSchema().format("int64")
            BigDecimal::class -> IntegerSchema().format("")
            Date::class -> DateSchema()
            LocalDate::class -> DateSchema()
            LocalDateTime::class -> DateTimeSchema()
            else -> ModelConverters.getInstance().read(clazz.java)[clazz.java.simpleName]
        }
    }

    private fun getEnumSchema(clazz: KClass<*>): Schema<*>? {
        val values = clazz.java.enumConstants ?: return null

        val schema = StringSchema()
        for (enumVal in values) {
            schema.addEnumItem(enumVal.toString())
        }
        return schema
    }

    fun modify(configure: OpenAPI.() -> Unit) {
        if (this.api == null) {
            throw Error("Schema has not been set")
        }
        if (finalized) {
            throw Error("Application startup has completed. You cannot modify the API object any more")
        }

        synchronized(this.api!!) {
            this.api!!.configure()
        }
    }
}
