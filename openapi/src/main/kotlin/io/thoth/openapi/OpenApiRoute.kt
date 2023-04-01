package io.thoth.openapi

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.routing.*
import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.FileResponse
import io.thoth.openapi.responses.RedirectResponse
import io.thoth.openapi.schema.ClassType
import io.thoth.openapi.schema.generateSchema
import io.thoth.openapi.schema.parent
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
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

    data class PathParameter(val name: String, val type: ClassType, val origin: ClassType)
    data class QueryParameter(val name: String, val type: ClassType, val origin: ClassType, val optional: Boolean)

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
        extractAllQueryParams(requestParamsType).map { it to it.type.generateSchema(false).first }
    }

    val pathParameters by lazy {
        extractAllPathParams(requestParamsType).map { it to it.type.generateSchema(false).first }
    }
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

    val description by lazy { requestParamsType.clazz.findAnnotation<Description>()?.description }

    val summary by lazy { requestParamsType.clazz.findAnnotation<Summary>()?.summary }

    val secured by lazy { requestParamsType.clazz.findAnnotationUp<Secured>() }

    val requestBody by lazy { requestBodyType.generateSchema() }

    val responseBody by lazy { responseBodyType.generateSchema() }

    val responseDescription by lazy { responseBodyType.clazz.findAnnotation<Description>() }

    val bodyDescription by lazy { requestBodyType.clazz.findAnnotation<Description>() }

    val responseContentType by lazy { getContentType(requestBodyType) }

    val requestContentType by lazy { getContentType(requestBodyType) }

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

    val tags by lazy { requestParamsType.clazz.findAnnotationsFirstUp<Tagged>().map { it.name } }

    init {
        assertParamsHierarchy()
    }

    private fun assertParamsHierarchy(paramsClazz: KClass<*> = requestParamsType.clazz) {
        // There are three conditions under which the hierarchy is valid:
        // 1. Every class over params is decorated as @Resource
        paramsClazz.findAnnotation<Resource>()
            ?: throw IllegalStateException("Class ${paramsClazz.qualifiedName} is not decorated as a resource")

        if (paramsClazz.parent == null) return

        val parentClass = paramsClazz.parent!!.java.kotlin
        // 2. Every class has a field which references the parent class name, because otherwise the
        // ktor routing will not work as one would think
        val hasDeclaredParent =
            paramsClazz.properties.map { it.returnType.classifier as KClass<*> }.any { it == parentClass }
        if (!hasDeclaredParent) {
            throw IllegalStateException(
                "Class ${paramsClazz.qualifiedName} has no property of type ${parentClass.qualifiedName}." +
                    "You have to create an additional property with the parent class as type",
            )
        }
        // 3. Every parent fulfills requirements 1 and 2
        assertParamsHierarchy(parentClass)
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
        params.clazz.parent?.java?.kotlin?.run { extractAllPathParams(ClassType.wrap(this), takenParams) }
        return takenParams.values.toList()
    }

    private fun extractPathParamsForClass(params: ClassType): List<PathParameter> {
        val pathParams = mutableListOf<PathParameter>()
        val resourcePath = params.clazz.findAnnotation<Resource>()!!.path
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
                PathParameter(name = varName, type = params.fromMember(varMember), origin = params),
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
        params.clazz.parent?.run { extractAllQueryParams(ClassType.wrap(this), takenParams) }
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
                    it.returnType.classifier != params.clazz.parent
                }
                .map {
                    QueryParameter(
                        name = it.name,
                        type = params.fromMember(it),
                        origin = params,
                        optional = it.optional,
                    )
                }
        return queryParams
    }

    private fun getContentType(classType: ClassType): String {
        if (classType.isEnum) return "text/plain"
        return when (classType.clazz) {
            // Binary
            BinaryResponse::class -> "application/octet-stream"
            ByteArray::class -> "application/octet-stream"
            FileResponse::class -> "application/octet-stream"
            // Redirect
            RedirectResponse::class -> "text/plain"
            // Primitives
            String::class -> "text/plain"
            Int::class -> "text/plain"
            Long::class -> "text/plain"
            Double::class -> "text/plain"
            Float::class -> "text/plain"
            Boolean::class -> "text/plain"
            ULong::class -> "text/plain"
            List::class -> "text/plain"
            Date::class -> "text/plain"
            LocalDate::class -> "text/plain"
            LocalDateTime::class -> "text/plain"
            BigDecimal::class -> "text/plain"
            UUID::class -> "text/plain"
            // Complex
            Map::class -> "application/json"
            Unit::class -> "application/json"
            else -> "application/json"
        }
    }
}
