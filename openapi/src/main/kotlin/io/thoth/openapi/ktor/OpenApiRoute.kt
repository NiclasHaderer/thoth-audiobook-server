package io.thoth.openapi.ktor

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.routing.*
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.InternalAPI
import io.thoth.openapi.common.fullPath
import io.thoth.openapi.common.optional
import io.thoth.openapi.common.parent
import io.thoth.openapi.ktor.schema.generateSchemas
import io.thoth.openapi.ktor.schema.toNamed
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class OpenApiRoute(
    val method: HttpMethod,
    val basePath: String,
    val requestParamsType: ClassType,
    val requestBodyType: ClassType,
    val responseBodyType: ClassType
) {

    constructor(
        method: HttpMethod,
        route: Route,
        params: ClassType,
        request: ClassType,
        response: ClassType
    ) : this(method, route.fullPath(), params, request, response)

    interface Parameter {
        val name: String
        val type: ClassType
        val origin: ClassType
        val optional: Boolean
    }

    data class PathParameter(
        override val name: String,
        override val type: ClassType,
        override val origin: ClassType,
    ) : Parameter {
        override val optional = false
    }

    data class QueryParameter(
        override val name: String,
        override val type: ClassType,
        override val origin: ClassType,
        override val optional: Boolean
    ) : Parameter

    companion object {
        inline fun <reified PARAMS, reified BODY, reified RESPONSE> create(
            method: HttpMethod,
            route: Route
        ): OpenApiRoute {
            return OpenApiRoute(
                method = method,
                route = route,
                params = ClassType.create<PARAMS>(),
                request = ClassType.create<BODY>(),
                response = ClassType.create<RESPONSE>(),
            )
        }
    }

    val queryParameters by lazy {
        extractAllQueryParams(requestParamsType).map { it to generateSchemas(it.type).toNamed() }
    }

    val pathParameters by lazy {
        extractAllPathParams(requestParamsType).map { it to generateSchemas(it.type).toNamed() }
    }

    @OptIn(InternalAPI::class)
    val resourcePath by lazy {
        var resourcePath = ""
        var resourceClass: KClass<*>? = this.requestParamsType.clazz
        while (resourceClass != null) {
            // Init has already checked that every class is decorated as a resource
            val resource = resourceClass!!.findAnnotation<Resource>()!!
            if (resource.path.isNotBlank()) {
                resourcePath =
                    if (resourcePath.isNotBlank()) {
                        "${resource.path}/$resourcePath"
                    } else {
                        resource.path
                    }
            }
            resourceClass = resourceClass!!.parent
        }
        resourcePath.ifBlank { null }
    }

    val description by lazy { requestParamsType.findAnnotation<Description>()?.description }

    val summary by lazy {
        requestParamsType.findAnnotations<Summary>().firstOrNull { it.method == this.method.value }?.summary
    }

    val secured by lazy {
        if (requestParamsType.findAnnotation<NotSecured>() != null) {
            null
        } else {
            requestParamsType.findAnnotationUp<Secured>()
        }
    }

    val requestBody by lazy { generateSchemas(requestBodyType).toNamed() }

    val responseBody by lazy { generateSchemas(responseBodyType).toNamed() }

    val responseDescription by lazy { responseBodyType.findAnnotation<Description>() }

    val bodyDescription by lazy { requestBodyType.findAnnotation<Description>() }

    val responseContentType by lazy { responseBody.first.contentType }

    val requestContentType by lazy { requestBody.first.contentType }

    val responseStatusCode by lazy {
        if (responseBodyType.clazz == Unit::class) {
            HttpStatusCode.NoContent
        } else if (method == HttpMethod.Post) {
            HttpStatusCode.Created
        } else {
            HttpStatusCode.OK
        }
    }

    val fullPath by lazy {
        var fullPath = basePath
        if (resourcePath != null) {
            fullPath += "/$resourcePath"
        }
        fullPath.replace("/+".toRegex(), "/")
    }

    val tags by lazy { requestParamsType.findAnnotationsFirstUp<Tagged>().map { it.name } }

    init {
        assertParamsHierarchy()
    }

    private fun assertParamsHierarchy(paramsClassType: ClassType = requestParamsType) {
        val paramsClazz = paramsClassType.clazz
        // There are three conditions under which the hierarchy is valid:
        // 1. Every class over params is decorated as @Resource
        paramsClazz.findAnnotation<Resource>()
            ?: throw IllegalStateException(
                "Class ${paramsClassType.clazz.qualifiedName} is not decorated as a resource",
            )

        if (paramsClassType.parent == null) return
        val parentClassType = paramsClassType.parent!!

        // 2. Every class has a field which references the parent class name, because otherwise the
        // ktor routing will not work as one would think
        val hasDeclaredParent =
            paramsClassType.properties.map { it.returnType.classifier as KClass<*> }.any { it == parentClassType.clazz }
        if (!hasDeclaredParent) {
            throw IllegalStateException(
                "Class ${paramsClazz.qualifiedName} has no property of type ${parentClassType.clazz.qualifiedName}." +
                    "You have to create an additional property with the parent class as type",
            )
        }
        // 3. Every parent fulfills requirements 1 and 2
        assertParamsHierarchy(parentClassType)
    }

    private fun extractAllPathParams(
        params: ClassType,
        takenParams: MutableMap<String, PathParameter> = mutableMapOf()
    ): List<PathParameter> {
        val pathParams = extractPathParamsForClass(params)
        for (param in pathParams) {
            // Check if the variable name is already taken
            if (takenParams.containsKey(param.name)) {
                throw IllegalStateException(
                    "Class ${params.clazz.qualifiedName} has a duplicate path parameter name ${param.name}. " +
                        "The parameter is already taken by ${takenParams[param.name]!!.origin.clazz.qualifiedName}",
                )
            }
        }
        takenParams.putAll(pathParams.associateBy { it.name })

        // Go up and check the parent
        params.parent?.run { extractAllPathParams(this, takenParams) }
        return takenParams.values.toList()
    }

    private fun extractPathParamsForClass(params: ClassType): List<PathParameter> {
        val pathParams = mutableListOf<PathParameter>()
        val resourcePath = params.findAnnotation<Resource>()!!.path
        val matches = "\\{((?:[a-z]|[A-Z]|_)+)}".toRegex().findAll(resourcePath)
        for (match in matches) {
            val varName = match.groupValues[1]
            // Check if the variable name is a valid kotlin variable name
            val varMember =
                params.properties.find { it.name == varName }
                    ?: throw IllegalStateException(
                        "Class ${params.clazz.qualifiedName} has a path parameter $varName which is not declared as a member. " +
                            "You have to create a property with the name $varName",
                    )
            pathParams.add(
                PathParameter(name = varName, type = params.forMember(varMember), origin = params),
            )
        }
        return pathParams
    }

    private fun extractAllQueryParams(
        params: ClassType,
        takenParams: MutableMap<String, QueryParameter> = mutableMapOf()
    ): List<QueryParameter> {
        val queryParams = extractQueryParamsForClass(params)
        for (param in queryParams) {
            if (param.name in takenParams) {
                throw IllegalStateException(
                    "Class ${params.clazz.qualifiedName} has a query parameter " +
                        "called ${param.name} which is also used in ${takenParams[param.name]!!.origin.clazz.qualifiedName}. " +
                        "Do not used duplicate parameters",
                )
            }
        }

        takenParams.putAll(queryParams.associateBy { it.name })
        params.parent?.run { extractAllQueryParams(this, takenParams) }
        return takenParams.values.toList()
    }

    private fun extractQueryParamsForClass(params: ClassType): List<QueryParameter> {
        val pathParams = extractPathParamsForClass(params).map { it.name }.toSet()
        val queryParams =
            params.properties
                .filter {
                    // Remove path parameters
                    it.name !in pathParams
                }
                .filter {
                    // Remove injected parent
                    it.returnType.classifier != params.parent?.clazz
                }
                .map {
                    QueryParameter(
                        name = it.name,
                        type = params.forMember(it),
                        origin = params,
                        optional = it.optional,
                    )
                }
        return queryParams
    }
}
