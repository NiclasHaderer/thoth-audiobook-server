package io.thoth.generators.openapi

import io.ktor.http.*
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement

object SchemaHolder {
    private val _api: OpenAPI =
        OpenAPI().also {
            it.components = Components()
            it.components.schemas = mutableMapOf()
            it.paths = Paths()
            it.info = Info()
        }

    val api: OpenAPI
        get() = _api

    fun json(): String {
        return Json.mapper().writeValueAsString(_api)
    }

    fun yaml(): String {
        return Yaml.mapper().writeValueAsString(_api)
    }

    fun addRouteToApi(route: OpenApiRoute) {
        val operation = getOperation(route)
        addPathAndQueryParameters(operation, route)
        val method = route.method
        if (
            method != HttpMethod.Get &&
                method != HttpMethod.Head &&
                method != HttpMethod.Delete &&
                method != HttpMethod.Options
        ) {
            addRequestBody(route, operation)
        }
        addResponse(route, operation)
    }

    private fun addPathAndQueryParameters(operation: Operation, route: OpenApiRoute) {
        for ((param, schema) in route.pathParameters) {
            _api.components.schemas.putAll(schema.second)
            operation.addParametersItem(
                Parameter().`in`("path").name(param.name).schema(schema.first.reference()),
            )
        }
        for ((param, schema) in route.queryParameters) {
            _api.components.schemas.putAll(schema.second)
            operation.addParametersItem(
                Parameter().`in`("query").name(param.name).schema(schema.first.reference()).required(!param.optional),
            )
        }
    }

    private fun addResponse(route: OpenApiRoute, operation: Operation) {
        val (responseSchema, responseNamedSchemas) = route.responseBody
        _api.components.schemas.putAll(responseNamedSchemas)
        operation.responses =
            ApiResponses()
                .addApiResponse(
                    route.responseStatusCode.value.toString(),
                    ApiResponse()
                        .description(route.responseDescription?.description ?: "")
                        .content(
                            Content()
                                .addMediaType(
                                    route.responseContentType.contentType,
                                    MediaType()
                                        .schema(
                                            responseSchema.reference(),
                                        ),
                                ),
                        ),
                )
    }

    private fun getOperation(route: OpenApiRoute): Operation {
        val pathItem = _api.paths.getOrPut(route.fullPath) { PathItem() }

        // Apply tags
        val operation = Operation().tags(route.tags)

        // Apply description and summary
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
            // Check if security scheme is already defined
            if (!_api.components.securitySchemes.containsKey(route.secured!!.name)) {
                throw IllegalStateException("Security scheme ${route.secured!!.name} is not defined")
            }
            operation.addSecurityItem(SecurityRequirement().addList(route.secured!!.name))
        }

        return operation
    }

    private fun addRequestBody(route: OpenApiRoute, operation: Operation) {
        val (bodySchema, bodyNamedSchemas) = route.requestBody
        _api.components.schemas.putAll(bodyNamedSchemas)
        operation.requestBody(
            RequestBody()
                .description(route.bodyDescription?.description)
                .content(
                    Content()
                        .addMediaType(
                            route.requestContentType.contentType,
                            MediaType()
                                .schema(
                                    bodySchema.reference(),
                                ),
                        ),
                ),
        )
    }
}
