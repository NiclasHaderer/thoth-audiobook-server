package io.thoth.openapi

import io.ktor.http.*
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.thoth.openapi.schema.ClassType
import io.thoth.openapi.schema.generateSchema

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
        return Json.mapper().writeValueAsString(this._api)
    }

    fun yaml(): String {
        return Yaml.mapper().writeValueAsString(this._api)
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
        val extractedPathParams = route.pathParameters
        for (param in extractedPathParams) {
            operation.addParametersItem(
                Parameter().also {
                    it.`in` = "path"
                    it.name = param.name
                    it.schema = ClassType.wrap(param.type).generateSchema(false).first
                },
            )
        }
        val extractedQueryParameters = route.queryParameters
        for (param in extractedQueryParameters) {
            operation.addParametersItem(
                Parameter().also {
                    it.`in` = "query"
                    it.name = param.name
                    it.required = !param.optional
                    it.schema = ClassType.wrap(param.type).generateSchema(false).first
                },
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
                        .also { it.description(route.responseDescription) }
                        .content(
                            Content()
                                .addMediaType(
                                    route.responseContentType,
                                    MediaType()
                                        .schema(
                                            responseSchema,
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

            operation.security =
                mutableListOf(
                    SecurityRequirement().also { it.addList(route.secured!!.name) },
                )
        }

        return operation
    }

    private fun addRequestBody(route: OpenApiRoute, operation: Operation) {
        val (bodySchema, bodyNamedSchemas) = route.requestBody
        _api.components.schemas.putAll(bodyNamedSchemas)
        operation.requestBody(
            RequestBody()
                .also { it.description(route.bodyDescription?.description) }
                .content(
                    Content()
                        .addMediaType(
                            route.requestContentType,
                            MediaType()
                                .schema(
                                    bodySchema,
                                ),
                        ),
                ),
        )
    }
}
