package io.thoth.openapi.client.typescript

import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.client.common.mappedTsReference
import io.thoth.openapi.common.getResourceContent
import io.thoth.openapi.common.padLinesStart
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.OpenApiRouteCollector
import java.io.File
import java.nio.file.Path
import mu.KotlinLogging.logger

class TsClientGenerator(override val routes: List<OpenApiRoute>, dist: Path, fileWriter: ((File, String) -> Unit)?) :
    ClientGenerator(dist, fileWriter) {
    private val log = logger {}
    private val typeDefinitions = mutableMapOf<String, TsGenerator.ReferenceType>()
    private val clientFunctions = mutableListOf<String>()
    private val requestRunner: String by lazy { getResourceContent("/client.ts") }
    private val utilityTypes by lazy { getResourceContent("/utility-types.ts") }

    init {
        generateApiClient()
    }

    private fun getParameters(route: OpenApiRoute): String {
        val urlParams = route.queryParameters + route.pathParameters
        val urlParamsStr =
            urlParams.map { (param) ->
                val (actual, all) = TsGenerator.generateTypes(param.type)
                typeDefinitions.putAll(all.mappedTsReference())
                "${param.name}${if (param.optional) "?" else ""}: ${actual.reference()}"
            }

        val urlParamsDecompositionStr = "{${urlParams.joinToString(", ") { it.first.name }}}"

        val bodyParamString =
            if (route.requestBodyType.clazz != Unit::class) {
                val (actual, all) = TsGenerator.generateTypes(route.requestBodyType)
                typeDefinitions.putAll(all.mappedTsReference())
                "body: ${actual.reference()}"
            } else ""

        return buildString {
            if (urlParamsStr.isNotEmpty()) append("$urlParamsDecompositionStr: {${urlParamsStr.joinToString(",") { it }}}, ")
            if (bodyParamString.isNotEmpty()) append("$bodyParamString, ")
            append("headers: HeadersInit = {}, interceptors: ApiInterceptor[] = []")
        }
    }

    private fun createURL(route: OpenApiRoute): String {
        // Add them to the URLSearchParams
        val finalPath = route.fullPath.replace("{", "\${")
        return if (route.queryParameters.isEmpty()) {
            "`$finalPath`"
        } else {
            "_createUrl(`$finalPath`, {${route.queryParameters.joinToString(", ") { it.first.name }}})"
        }
    }

    private fun generateApiClient() {
        routes.forEach { route ->
            val routeName = getRouteName(route)
            if (routeName == null) {
                log.warn("Route ${route.method}:${route.fullPath} has no summary")
                return@forEach
            }
            val (responseBody, all) = TsGenerator.generateTypes(route.responseBodyType)
            typeDefinitions.putAll(all.mappedTsReference())

            val function = buildString {
                append("$routeName(${getParameters(route)}): Promise<ApiResponse<${responseBody.reference()}>> {\n")
                append("  const headersImpl = new Headers(headers)\n")
                append("  defaultHeadersImpl.forEach((value, key) => headersImpl.append(key, value))\n")
                append("  return _request(${createURL(route)}, \"${route.method.value}\", \"${responseBody.parser.methodName}\", ")
                append("headersImpl, ")
                append(if (route.requestBodyType.clazz != Unit::class) "body" else "undefined")
                append(", [...defaultInterceptors, ...interceptors], executor, ${route.secured != null});\n")
                append("}")
            }.padLinesStart(' ', 4)
            clientFunctions.add(function)
        }
    }

    private fun createTypeImports(): String {
        return buildString {
            append("import {ApiCallData, ApiInterceptor, ApiResponse, _request, _createUrl} from \"./client\";\n")
            append("import type {")
            append(typeDefinitions.keys.sorted().joinToString(", "))
            append("} from \"./models\";\n")
        }
    }

    private fun getClientRequests(): String {
        return buildString {
            append("// noinspection JSUnusedGlobalSymbols,ES6UnusedImports\n")
            append(createTypeImports())
            append("export const createApi = (\n")
            append("  defaultHeaders: HeadersInit = {},\n")
            append("  defaultInterceptors: ApiInterceptor[] = [],\n")
            append("  executor = (callData: ApiCallData) => fetch(callData.route, {method: callData.method, headers: callData.headers, body: callData.bodySerializer(callData.body)})\n")
            append(") => {\n")
            append("  const defaultHeadersImpl = new Headers(defaultHeaders)\n")
            append("  return {\n")
            append(clientFunctions.joinToString(",\n") { it })
            append("\n")
            append("  } as const;\n")
            append("}\n")
        }
    }

    private fun getClientTypes(): String {
        return buildString {
            append("import type { Pair } from \"./utility-types\";\n")
            append(typeDefinitions.values.joinToString("\n\n") { "export ${it.content()}" })
        }
    }

    override fun generateClient(): List<ClientPart> {
        val clientTypes = getClientTypes()
        val clientRequests = getClientRequests()
        return listOf(
            ClientPart("client.ts", requestRunner),
            ClientPart("utility-types.ts", utilityTypes),
            ClientPart("models.ts", clientTypes),
            ClientPart("api.ts", clientRequests),
        )
    }
}

fun generateTsClient(
    dist: Path,
    routes: List<OpenApiRoute> = OpenApiRouteCollector.values(),
    fileWriter: ((File, String) -> Unit)? = null
) {
    TsClientGenerator(routes = routes, dist = dist, fileWriter = fileWriter).safeClient()
}

fun generateTsClient(
    dist: String,
    routes: List<OpenApiRoute> = OpenApiRouteCollector.values(),
    fileWriter: ((File, String) -> Unit)? = null
) = generateTsClient(Path.of(dist), routes, fileWriter)
