package io.thoth.openapi.client.typescript

import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.common.getResourceContent
import io.thoth.openapi.common.padLinesStart
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.OpenApiRouteCollector
import java.nio.file.Path
import mu.KotlinLogging.logger

class TsClientGenerator(override val routes: List<OpenApiRoute>, dist: Path) : ClientGenerator(dist) {
    private val typeDefinitions = mutableMapOf<String, TsGenerator.Type>()
    private val clientFunctions = mutableListOf<String>()
    private val log = logger {}
    private val requestRunner: String by lazy { getResourceContent("/client.ts") }

    init {
        generateApiClient()
    }

    private fun getParameters(route: OpenApiRoute): String {
        val urlParams = route.queryParameters + route.pathParameters
        val urlParamsStr =
            urlParams.map { (param) ->
                val (actual, all) = TsGenerator.generateTypes(param.type)
                typeDefinitions.putAll(
                    all.filterIsInstance<TsGenerator.ReferenceType>().associateBy { it.reference() },
                )
                "${param.name}${if (param.optional) "?" else ""}: ${actual.reference()}"
            }

        val urlParamsDecompositionStr =
            "{${
                urlParams.joinToString(", ") {
                    it.first.name
                }
            }}"

        val bodyParamString =
            if (route.requestBodyType.clazz != Unit::class) {
                val (actual, all) = TsGenerator.generateTypes(route.requestBodyType)
                typeDefinitions.putAll(
                    all.filterIsInstance<TsGenerator.ReferenceType>().associateBy { it.reference() },
                )
                "body: ${actual.reference()}"
            } else ""

        return "${
            if (urlParamsStr.isEmpty()) {
                ""
            } else {
                "$urlParamsDecompositionStr: {${urlParamsStr.joinToString(",") { it }}}, "
            }
        }${
            if (bodyParamString.isEmpty()) ""
            else {
                "$bodyParamString, "
            }
        } headers: HeadersInit = {}, interceptors: ApiInterceptor[] = []"
            .trim()
    }

    private fun createURL(route: OpenApiRoute): String {
        // Add them to the URLSearchParams
        val finalPath = route.fullPath.replace("{", "\${")
        return """
        ${
            if (route.queryParameters.isEmpty()) {
                "`$finalPath`"
            } else {
                "_createUrl(`$finalPath`, {${route.queryParameters.joinToString(", ") { it.first.name }}})"
            }
        }
    """
            .trimIndent()
    }

    private fun generateApiClient() {
        routes.forEach { route ->
            val routeName = getRouteName(route)
            if (routeName == null) {
                log.warn("Route ${route.method}:${route.fullPath} has no summary")
                return@forEach
            }
            val responseBody = TsGenerator.generateTypes(route.responseBodyType).first
            val function =
                """
            $routeName(${getParameters(route)}): Promise<ApiResponse<${responseBody.reference()}>> {
              const headersImpl = new Headers(headers)
              defaultHeadersImpl.forEach((value, key) => headersImpl.append(key, value))
              return _request(${createURL(route)}, "${route.method.value}", "${
                  responseBody.parser.methodName
              }", headersImpl, ${
                  if (route.requestBodyType.clazz != Unit::class) "body" else "undefined"
              }, [...defaultInterceptors, ...interceptors], 
              executor,
              ${
                  route.secured != null
              });
            }
        """
                    .trimIndent()
            clientFunctions.add(function.padLinesStart(' ', 4))

            val responseInterfaces = TsGenerator.generateTypes(route.responseBodyType)
            typeDefinitions.putAll(
                responseInterfaces.second.filterIsInstance<TsGenerator.ReferenceType>().associateBy { it.reference() },
            )
        }
    }

    private fun createTypeImports(): String {
        val modelImports = "import type {${
            typeDefinitions.values.asSequence().filterIsInstance<TsGenerator.ReferenceType>()
                .map {
                    // Replace the generic <> with nothing to not break the import
                    it.reference().replace("<.*>".toRegex(), "")
                }.distinct().sorted().joinToString(", ")
        }} from \"./types\";\n"
        val apiImports = "import {ApiCallData, ApiInterceptor, ApiResponse, _request, _createUrl} from \"./client\";\n"
        return modelImports + apiImports + "\n"
    }

    private fun getClientRequests(): String {
        return createTypeImports() +
            """
            export const createApi = (
              defaultHeaders: HeadersInit = {},
              defaultInterceptors: ApiInterceptor[] = [],
              executor = (callData: ApiCallData) => fetch(callData.route, {method: callData.method, headers: callData.headers, body: callData.bodySerializer(callData.body)})
            ) => {
              const defaultHeadersImpl = new Headers(defaultHeaders)
              return {
            """
                .trimIndent() +
            "\n" +
            clientFunctions.joinToString(",\n") { it } +
            "\n" +
            """
              } as const;
            }
            """
                .trimIndent()
    }

    private fun getClientTypes(): String {
        val referenceTypes = typeDefinitions.values.filterIsInstance<TsGenerator.ReferenceType>()
        return referenceTypes.joinToString("\n\n") { "export ${it.content()}" }
    }

    override fun generateClient(): List<ClientPart> {
        val clientTypes = getClientTypes()
        val clientRequests = getClientRequests()
        return listOf(
            ClientPart("client.ts", requestRunner),
            ClientPart("types.ts", clientTypes),
            ClientPart("api.ts", clientRequests),
        )
    }
}

fun generateTsClient(dist: Path, routes: List<OpenApiRoute> = OpenApiRouteCollector.values()) {
    TsClientGenerator(routes = routes, dist = dist).safeClient()
}

fun generateTsClient(dist: String, routes: List<OpenApiRoute> = OpenApiRouteCollector.values()) =
    generateTsClient(Path.of(dist), routes)
