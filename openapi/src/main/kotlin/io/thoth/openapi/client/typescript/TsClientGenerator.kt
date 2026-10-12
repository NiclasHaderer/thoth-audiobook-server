package io.thoth.openapi.client.typescript

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.ktor.server.application.Application
import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.client.common.mappedTsReference
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.getResourceContent
import io.thoth.openapi.common.padLinesStart
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.plugins.OpenAPIConfigurationKey
import java.io.File
import java.nio.file.Path

class TsClientGenerator(
    override val routes: List<OpenApiRoute>,
    private val webSockets: List<OpenApiRoute>,
    val apiFactoryName: String,
    private val apiVersion: String,
    dist: Path,
    fileWriter: ((File, String) -> Unit)?,
    typePackages: List<String>,
    cleanDistPackage: Boolean,
) : ClientGenerator(dist, fileWriter, cleanDistPackage) {
    private val log = logger {}
    private val typeDefinitions = mutableMapOf<String, TsTypeGenerator.TsReferenceType>()
    private val clientFunctions = mutableListOf<String>()
    private val requestRunner: String by lazy { getResourceContent("/typescript/client.ts") }
    private val utilityTypes by lazy { getResourceContent("/typescript/utility-types.ts") }
    private val typeProvider =
        TypeGenerator.Provider(TsTypeGenerator::class, listOf("io.thoth.openapi.client.typescript") + typePackages)

    init {
        generateApiClient()
    }

    private fun reference(type: ClassType): String {
        val (actual, all) = typeProvider.generateTypes(type)
        typeDefinitions.putAll(all.mappedTsReference())
        return actual.reference()
    }

    private fun urlParameters(route: OpenApiRoute): String? {
        val urlParams = (route.queryParameters + route.pathParameters).map { it.first }
        if (urlParams.isEmpty()) return null
        val types = urlParams.joinToString(",") { "${it.name}${if (it.optional) "?" else ""}: ${reference(it.type)}" }
        return "{${urlParams.joinToString(", ") { it.name }}}: {$types}"
    }

    private fun getParameters(route: OpenApiRoute): String =
        buildString {
            urlParameters(route)?.let { append("$it, ") }
            if (route.requestBodyType.clazz != Unit::class) append("body: ${reference(route.requestBodyType)}, ")
            append("headers: HeadersInit = {}, interceptors: ApiInterceptor[] = []")
        }

    private fun messageType(type: ClassType): String = if (type.clazz == Unit::class) "never" else reference(type)

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
                log.warn { "Route ${route.method}:${route.fullPath} has no summary" }
                return@forEach
            }
            val (responseBody, all) = typeProvider.generateTypes(route.responseBodyType)
            typeDefinitions.putAll(all.mappedTsReference())

            val function =
                buildString {
                    append(
                        "$routeName: (${
                            getParameters(
                                route,
                            )
                        }): Promise<ApiResponse<${responseBody.reference()}>> => {\n",
                    )
                    append(
                        "  return _request(${
                            createURL(
                                route,
                            )
                        }, \"${route.method.value}\", \"${responseBody.parser().methodName}\", ",
                    )
                    append("_mergeHeaders(defaultHeadersImpl, headers), ")
                    append(if (route.requestBodyType.clazz != Unit::class) "body" else "undefined")
                    append(", [...defaultInterceptors, ...interceptors], executor, ${route.secured != null});\n")
                    append("}")
                }.padLinesStart(' ', 4)
            clientFunctions.add(function)
        }
        webSockets.forEach { route ->
            val routeName = getRouteName(route)
            if (routeName == null) {
                log.warn { "Websocket ${route.fullPath} has no summary" }
                return@forEach
            }
            val socket = "ApiSocket<${messageType(route.requestBodyType)}, ${messageType(route.responseBodyType)}>"
            clientFunctions.add(
                buildString {
                    append("$routeName: (")
                    urlParameters(route)?.let { append("$it, ") }
                    append("interceptors: ApiSocketInterceptor[] = []): Promise<$socket> => {\n")
                    append("  return _socket(${createURL(route)}, ")
                    append(
                        "[...defaultSocketInterceptors, ...interceptors], socketExecutor, ${route.secured != null});\n",
                    )
                    append("}")
                }.padLinesStart(' ', 4),
            )
        }
    }

    private fun createTypeImports(): String =
        buildString {
            append(
                "import {ApiCallData, ApiInterceptor, ApiResponse, ApiSocket, ApiSocketData, ApiSocketInterceptor, _request, _createUrl, _mergeHeaders, _socket} from \"./client\";\n",
            )
            append("import type {")
            append(typeDefinitions.keys.sorted().joinToString(", "))
            append("} from \"./models\";\n")
        }

    private fun getClientRequests(): String =
        buildString {
            append("/* eslint-disable */\n")
            append("// noinspection JSUnusedGlobalSymbols,ES6UnusedImports\n")
            append("// noinspection ES6UnusedImports\n")
            append("// @ts-nocheck\n")

            append(createTypeImports())
            append("\n")
            append("export const $apiFactoryName = (\n")
            append("  defaultHeaders: HeadersInit = {},\n")
            append("  defaultInterceptors: ApiInterceptor[] = [],\n")
            append(
                "  executor = (callData: ApiCallData) => fetch(callData.route, {method: callData.method, headers: callData.headers, body: callData.bodySerializer(callData.body)}),\n",
            )
            append("  defaultSocketInterceptors: ApiSocketInterceptor[] = [],\n")
            append("  socketExecutor = (socketData: ApiSocketData) => new WebSocket(socketData.route)\n")
            append(") => {\n")
            append("  const defaultHeadersImpl = new Headers(defaultHeaders)\n")
            append("  return {\n")
            append("    apiVersion: \"$apiVersion\",\n")
            append(clientFunctions.joinToString(",\n") { it })
            append("\n")
            append("  } as const;\n")
            append("}")
        }

    private fun getClientTypes(): String =
        buildString {
            append("/* eslint-disable */\n")
            append("// @ts-nocheck\n")
            append("import type { Pair } from \"./utility-types\";\n\n")
            append(typeDefinitions.values.joinToString("\n\n") { "export ${it.content()}" })
        }

    override fun generateClient(): List<ClientPart> {
        val clientTypes = getClientTypes()
        val clientRequests = getClientRequests()
        return listOf(
            ClientPart("client.ts", requestRunner),
            ClientPart("utility-types.ts", utilityTypes),
            ClientPart("models.ts", clientTypes),
            ClientPart("api-client.ts", clientRequests),
        )
    }
}

fun Application.generateTsClient(
    dist: Path,
    apiFactoryName: String = "createApi",
    routes: List<OpenApiRoute>? = null,
    fileWriter: ((File, String) -> Unit)? = null,
    typePackages: List<String> = emptyList(),
    cleanDistPackage: Boolean = true,
) {
    val config = this.attributes[OpenAPIConfigurationKey]
    TsClientGenerator(
        routes = routes ?: config.routeCollector.values(),
        webSockets = config.routeCollector.webSockets,
        apiVersion = config.apiVersion,
        dist = dist,
        fileWriter = fileWriter,
        apiFactoryName = apiFactoryName,
        typePackages = typePackages,
        cleanDistPackage = cleanDistPackage,
    ).safeClient()
}

fun Application.generateTsClient(
    dist: String,
    apiFactoryName: String = "createApi",
    routes: List<OpenApiRoute>? = null,
    fileWriter: ((File, String) -> Unit)? = null,
    typePackages: List<String> = emptyList(),
    cleanDistPackage: Boolean = true,
) = generateTsClient(
    dist = Path.of(dist),
    routes = routes,
    fileWriter = fileWriter,
    apiFactoryName = apiFactoryName,
    typePackages = typePackages,
    cleanDistPackage = cleanDistPackage,
)
