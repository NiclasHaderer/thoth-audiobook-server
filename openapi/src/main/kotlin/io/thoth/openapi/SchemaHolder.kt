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
import io.thoth.openapi.schema.ClassType
import io.thoth.openapi.schema.ContentTypeLookup
import io.thoth.openapi.schema.generateSchema
import io.thoth.openapi.schema.getPathParameters
import kotlin.reflect.full.findAnnotation

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

    inline fun <reified PARAMS, reified BODY, reified RESPONSE> addRouteToApi(
        url: String,
        method: HttpMethod,
    ) {
        val body = ClassType.create<BODY>()
        val params = ClassType.create<PARAMS>()
        val response = ClassType.create<RESPONSE>()
        addRouteToApi(url, method, params, body, response)
    }

    fun addRouteToApi(
        url: String,
        method: HttpMethod,
        requestParams: ClassType,
        requestBody: ClassType,
        responseBody: ClassType,
    ) {
        val operation = getPath(url, method, requestParams)
        addPathAndQueryParameters(operation, requestParams)
        if (
            method != HttpMethod.Get &&
                method != HttpMethod.Head &&
                method != HttpMethod.Delete &&
                method != HttpMethod.Options
        ) {
            addRequest(requestBody, operation)
        }
        val statusCode = getStatusCode(method, responseBody)
        addResponse(responseBody, statusCode, operation)
    }

    private fun addPathAndQueryParameters(operation: Operation, pathParams: ClassType) {
        val extractedParams = getPathParameters(pathParams.clazz)
        for (param in extractedParams) {
            operation.addParametersItem(
                Parameter().also {
                    it.`in` = "path"
                    it.name = param.name
                    it.schema = ClassType.wrap(param.type).generateSchema(false).first
                },
            )
        }
    }

    private fun getStatusCode(method: HttpMethod, responseBody: ClassType): HttpStatusCode {
        return if (responseBody.clazz == Unit::class) {
            HttpStatusCode.NoContent
        } else if (method == HttpMethod.Post) {
            HttpStatusCode.Created
        } else {
            HttpStatusCode.OK
        }
    }

    private fun addResponse(response: ClassType, statusCode: HttpStatusCode, operation: Operation) {
        val (responseSchema, responseNamedSchemas) = response.generateSchema()
        _api.components.schemas.putAll(responseNamedSchemas)
        operation.responses =
            ApiResponses()
                .addApiResponse(
                    statusCode.value.toString(),
                    ApiResponse()
                        .also {
                            val description = response.clazz.findAnnotation<Description>()
                            it.description(description?.description ?: "")
                        }
                        .content(
                            Content()
                                .addMediaType(
                                    ContentTypeLookup.forClassType(response),
                                    MediaType()
                                        .schema(
                                            responseSchema,
                                        ),
                                ),
                        ),
                )
    }

    private fun getPath(url: String, method: HttpMethod, requestParams: ClassType): Operation {
        val pathItem = _api.paths.getOrPut(url) { PathItem() }
        val tags = requestParams.clazz.findAnnotationsFirstUp<Tagged>().map { it.name }

        val operation = Operation().tags(tags)
        operation.description(requestParams.clazz.findAnnotation<Description>()?.description)
        operation.summary(requestParams.clazz.findAnnotation<Summary>()?.summary)
        when (method) {
            HttpMethod.Get -> pathItem.get = operation
            HttpMethod.Post -> pathItem.post = operation
            HttpMethod.Put -> pathItem.put = operation
            HttpMethod.Patch -> pathItem.patch = operation
            HttpMethod.Delete -> pathItem.delete = operation
            HttpMethod.Head -> pathItem.head = operation
            HttpMethod.Options -> pathItem.options = operation
            else -> throw Error("Unsupported method")
        }
        return operation
    }

    private fun addRequest(body: ClassType, operation: Operation) {
        val (bodySchema, bodyNamedSchemas) = body.generateSchema()
        _api.components.schemas.putAll(bodyNamedSchemas)
        operation.requestBody(
            RequestBody()
                .also {
                    val description = body.clazz.findAnnotation<Description>()
                    it.description(description?.description)
                }
                .content(
                    Content()
                        .addMediaType(
                            ContentTypeLookup.forClassType(body),
                            MediaType()
                                .schema(
                                    bodySchema,
                                ),
                        ),
                ),
        )
    }
}
