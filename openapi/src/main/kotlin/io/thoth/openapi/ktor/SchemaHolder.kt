package io.thoth.openapi.ktor

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.RefUtils
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement

private const val ERROR_SCHEMA_NAME = "ErrorResponse"

class SchemaHolder {
    private val _api: OpenAPI =
        OpenAPI().also {
            it.components = Components()
            it.components.schemas = mutableMapOf()
            it.paths = Paths()
            it.info = Info()
        }

    val api: OpenAPI
        get() = _api

    fun json(): String = Json.mapper().writeValueAsString(_api)

    fun yaml(): String = Yaml.mapper().writeValueAsString(_api)

    fun addRouteToApi(route: OpenApiRoute) {
        val operation = getOperation(route)
        addPathAndQueryParameters(operation, route)
        val method = route.method
        if (
            method != HttpMethod.Get &&
            method != HttpMethod.Head &&
            method != HttpMethod.Delete &&
            method != HttpMethod.Options &&
            route.requestBodyType.clazz != Unit::class
        ) {
            addRequestBody(route, operation)
        }
        addResponse(route, operation)
        addErrorResponses(route, operation)
    }

    private fun addPathAndQueryParameters(
        operation: Operation,
        route: OpenApiRoute,
    ) {
        for ((param, schema) in route.pathParameters) {
            _api.components.schemas.putAll(schema.second)
            operation.addParametersItem(
                Parameter()
                    .`in`("path")
                    .name(param.name)
                    .description(param.description)
                    .schema(schema.first.reference())
                    .required(true),
            )
        }
        for ((param, schema) in route.queryParameters) {
            _api.components.schemas.putAll(schema.second)
            operation.addParametersItem(
                Parameter()
                    .`in`("query")
                    .name(param.name)
                    .description(param.description)
                    .schema(schema.first.reference())
                    .required(!param.optional),
            )
        }
    }

    private fun addResponse(
        route: OpenApiRoute,
        operation: Operation,
    ) {
        val (responseSchema, responseNamedSchemas) = route.responseBody
        val statusCode = route.responseStatusCode
        val response = ApiResponse().description(route.responseDescription?.description ?: statusCode.description)

        if (route.responseBodyType.clazz != Unit::class) {
            _api.components.schemas.putAll(responseNamedSchemas)
            response.content(
                Content()
                    .addMediaType(
                        route.responseContentType.toString(),
                        MediaType().schema(responseSchema.reference()),
                    ),
            )
        }

        operation.responses = ApiResponses().addApiResponse(statusCode.value.toString(), response)
    }

    private fun addErrorResponses(
        route: OpenApiRoute,
        operation: Operation,
    ) {
        if (route.secured != null) {
            operation.responses
                .addApiResponse(HttpStatusCode.Unauthorized.value.toString(), errorResponse(HttpStatusCode.Unauthorized))
                .addApiResponse(HttpStatusCode.Forbidden.value.toString(), errorResponse(HttpStatusCode.Forbidden))
        }
        operation.responses.setDefault(errorResponse(null))
    }

    private fun errorResponse(status: HttpStatusCode?): ApiResponse {
        _api.components.schemas.getOrPut(ERROR_SCHEMA_NAME) {
            ObjectSchema()
                .properties(
                    mapOf(
                        "error" to StringSchema(),
                        "status" to IntegerSchema().format("int32"),
                        "details" to Schema<Any>().nullable(true),
                    ),
                ).required(listOf("error", "status"))
        }

        return ApiResponse()
            .description(status?.description ?: "Unexpected error")
            .content(
                Content()
                    .addMediaType(
                        ContentType.Application.Json.toString(),
                        MediaType().schema(Schema<Any>().`$ref`(RefUtils.constructRef(ERROR_SCHEMA_NAME))),
                    ),
            )
    }

    private fun getOperation(route: OpenApiRoute): Operation {
        val pathItem = _api.paths.getOrPut(route.fullPath) { PathItem() }

        // Apply tags
        val operation = Operation().tags(route.tags)

        // Apply description and summary
        operation.operationId(route.operationId)
        operation.description(route.description)
        operation.summary(route.summary)

        // Map method to operation
        when (route.method) {
            HttpMethod.Get -> pathItem.get = operation
            HttpMethod.Post -> pathItem.post = operation
            HttpMethod.Put -> pathItem.put = operation
            HttpMethod.Patch -> pathItem.patch = operation
            HttpMethod.Delete -> pathItem.delete = operation
            HttpMethod.Head -> pathItem.head = operation
            HttpMethod.Options -> pathItem.options = operation
            else -> throw Error("Unsupported method")
        }

        // Apply security
        if (route.secured != null) {
            // Check if a security scheme is already defined
            if (!_api.components.securitySchemes.containsKey(route.secured!!.name)) {
                throw IllegalStateException("Security scheme ${route.secured!!.name} is not defined")
            }
            operation.addSecurityItem(SecurityRequirement().addList(route.secured!!.name))
        }

        return operation
    }

    private fun addRequestBody(
        route: OpenApiRoute,
        operation: Operation,
    ) {
        val (bodySchema, bodyNamedSchemas) = route.requestBody
        _api.components.schemas.putAll(bodyNamedSchemas)
        operation.requestBody(
            RequestBody()
                .description(route.bodyDescription?.description)
                .content(
                    Content()
                        .addMediaType(route.requestContentType.toString(), MediaType().schema(bodySchema.reference())),
                ),
        )
    }
}
