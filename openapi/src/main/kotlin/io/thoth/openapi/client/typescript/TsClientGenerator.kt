package io.thoth.openapi.client.typescript

import io.ktor.server.application.*
import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.client.common.mappedTsReference
import io.thoth.openapi.common.getResourceContent
import io.thoth.openapi.common.padLinesStart
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.plugins.OpenAPIConfigurationKey
import java.io.File
import java.nio.file.Path
import mu.KotlinLogging.logger

class TsClientGenerator(
    override val routes: List<OpenApiRoute>,
    val apiFactoryName: String,
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
        TypeGenerator.Provider(
            TsTypeGenerator::class,
            listOf("io.thoth.openapi.client.typescript") + typePackages,
        )

    init {
        generateApiClient()
    }

    private fun getParameters(route: OpenApiRoute): String {
        val urlParams = route.queryParameters + route.pathParameters
        val urlParamsStr =
            urlParams.map { (param) ->
                val (actual, all) = typeProvider.generateTypes(param.type)
                typeDefinitions.putAll(all.mappedTsReference())
                "${param.name}${if (param.optional) "?" else ""}: ${actual.reference()}"
            }

        val urlParamsDecompositionStr = "{${urlParams.joinToString(", ") { it.first.name }}}"

        val bodyParamString =
            if (route.requestBodyType.clazz != Unit::class) {
                val (actual, all) = typeProvider.generateTypes(route.requestBodyType)
                typeDefinitions.putAll(all.mappedTsReference())
                "body: ${actual.reference()}"
            } else ""

        return buildString {
            if (urlParamsStr.isNotEmpty())
                append("$urlParamsDecompositionStr: {${urlParamsStr.joinToString(",") { it }}}, ")
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
            val (responseBody, all) = typeProvider.generateTypes(route.responseBodyType)
            typeDefinitions.putAll(all.mappedTsReference())

            val function =
                buildString {
                        append(
                            "$routeName: (${getParameters(route)}): Promise<ApiResponse<${responseBody.reference()}>> => {\n"
                        )
                        append(
                            "  return _request(${createURL(route)}, \"${route.method.value}\", \"${responseBody.parser().methodName}\", "
                        )
                        append("_mergeHeaders(defaultHeadersImpl, headers), ")
                        append(if (route.requestBodyType.clazz != Unit::class) "body" else "undefined")
                        append(", [...defaultInterceptors, ...interceptors], executor, ${route.secured != null});\n")
                        append("}")
                    }
                    .padLinesStart(' ', 4)
            clientFunctions.add(function)
        }
    }

    private fun createTypeImports(): String {
        return buildString {
            append(
                "import {ApiCallData, ApiInterceptor, ApiResponse, _request, _createUrl, _mergeHeaders} from \"./client\";\n"
            )
            append("import type {")
            append(typeDefinitions.keys.sorted().joinToString(", "))
            append("} from \"./models\";\n")
        }
    }

    private fun getClientRequests(): String {
        return buildString {
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
                "  executor = (callData: ApiCallData) => fetch(callData.route, {method: callData.method, headers: callData.headers, body: callData.bodySerializer(callData.body)})\n"
            )
            append(") => {\n")
            append("  const defaultHeadersImpl = new Headers(defaultHeaders)\n")
            append("  return {\n")
            append(clientFunctions.joinToString(",\n") { it })
            append("\n")
            append("  } as const;\n")
            append("}")
        }
    }

    private fun getClientTypes(): String {
        return buildString {
            append("/* eslint-disable */\n")
            append("// @ts-nocheck\n")
            append("import type { Pair } from \"./utility-types\";\n\n")
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
    cleanDistPackage: Boolean = true
) {
    TsClientGenerator(
            routes = routes ?: this.attributes[OpenAPIConfigurationKey].routeCollector.values(),
            dist = dist,
            fileWriter = fileWriter,
            apiFactoryName = apiFactoryName,
            typePackages = typePackages,
            cleanDistPackage = cleanDistPackage
        )
        .safeClient()
}

fun Application.generateTsClient(
    dist: String,
    apiFactoryName: String = "createApi",
    routes: List<OpenApiRoute>? = null,
    fileWriter: ((File, String) -> Unit)? = null,
    typePackages: List<String> = emptyList(),
    cleanDistPackage: Boolean = true
) =
    generateTsClient(
        dist = Path.of(dist),
        routes = routes,
        fileWriter = fileWriter,
        apiFactoryName = apiFactoryName,
        typePackages = typePackages,
        cleanDistPackage = cleanDistPackage
    )
