package io.thoth.openapi.client.typescript

import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.client.typescript.types.TsGenerator
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.OpenApiRouteCollector
import java.nio.file.Path
import mu.KotlinLogging.logger

class TsClientGenerator(override val routes: List<OpenApiRoute>, dist: Path) : ClientGenerator(dist) {
    private val typeDefinitions = mutableMapOf<String, TsGenerator.Type>()
    private val clientFunctions = mutableListOf<String>()
    private val log = logger {}

    private companion object {

        private fun createUrlCreator(): String {
            return """
        type ArrayIsch<T> = T | T[]
        const __createUrl = (route: string, params: Record<string, ArrayIsch<string | number | boolean | undefined | null>>): string => {
          const finalUrlParams = new URLSearchParams()
          for (let [key, value] of Object.entries(params)) {
            if (Array.isArray(value)) {
              const newValue = value.filter(i => i !== "" && i !== undefined && i !== null) as (string | number | boolean)[]
              newValue.forEach(v => finalUrlParams.append(key, v.toString()))
            } else {
              if (value !== null && value !== undefined) {
                finalUrlParams.append(key, value.toString())
              }
            }
          }
          return finalUrlParams ? `$ {route}?$ {finalUrlParams.toString()}` : route
        };
    """
                .trimIndent()
                .replace("$ ", "$") + "\n"
        }

        private fun createRequestMaker(): String {
            return """
        const __request = async <T>(
                route: string, 
                method: string, 
                bodyParseMethod: "text" | "json" | "blob", 
                headers: Headers,
                body: object | undefined,
                interceptors: ApiInterceptor[],
                executor: ApiCallData["executor"],
                requiresAuth: boolean,
            ): Promise<ApiResponse<T>> => {
            if(!headers.has("Content-Type")) {
                headers.set("Content-Type", "application/json");
            }
            let apiCallData: ApiCallData = {
                route,
                method, 
                body, 
                headers, 
                bodySerializer: (body?: object) => body ? JSON.stringify(body) : undefined,
                executor,
                requiresAuth,
            };
            if (interceptors.length > 0) {
                for (const interceptor of interceptors) {
                    apiCallData = await interceptor(apiCallData);
                }
            }
                    
            return apiCallData.executor(apiCallData)
                .then(async (response) => {
                    if(!(response instanceof Response)) {
                        return response;
                    }
                
                    if (response.ok) {
                        return {
                            success: true,
                            body: await response[bodyParseMethod]()
                        } as const
                    } else {
                        return {
                            success: false,
                            status: response.status,
                            error: await response.json().catch(() => response.text()).catch(() => response.statusText)
                        } as const
                    }
                })
                .catch((error) => ({success: false, error: error?.toString()} as const))
        };
        """
                .trimIndent() + "\n"
        }
    }

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
                "__createUrl(`$finalPath`, {${route.queryParameters.joinToString(", ") { it.first.name }}})"
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
                return __request(${createURL(route)}, "${route.method.value}", "${
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
            clientFunctions.add(function)

            val responseInterfaces = TsGenerator.generateTypes(route.responseBodyType)
            typeDefinitions.putAll(
                responseInterfaces.second.filterIsInstance<TsGenerator.ReferenceType>().associateBy { it.reference() },
            )
        }
    }

    private fun createTypeImports(): String {
        return "import type {${
            typeDefinitions.values.asSequence().filterIsInstance<TsGenerator.ReferenceType>()
                .map {
                    // Replace the generic <> with nothing to not break the import
                    it.reference().replace("<.*>".toRegex(), "")
                }.distinct().sorted().joinToString(", ") + ", ApiResponse, ApiInterceptor, ApiCallData"
        }} from \"./types\";\n"
    }

    private fun getClientRequests(): String {
        return createTypeImports() +
            createUrlCreator() +
            createRequestMaker() +
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
            clientFunctions.joinToString(
                ",\n",
            ) {
                "  $it"
            } +
            """
              } as const;
            }
            """
                .trimIndent()
    }

    private fun getClientTypes(): String {
        val internalTypes =
            """
            export type ApiError = {
                success: false,
                error: string | object
                status?: number
            }
            
            export type ApiSuccess<T> = {
                success: true,
                body: T
            }

            export type ApiResponse<T> = ApiError | ApiSuccess<T>
            
            export type ApiCallData = {
                requiresAuth: boolean,
                route: string,
                method: string,
                body?: object,
                headers: Headers,
                bodySerializer: (body?: object) => string | undefined,
                executor: (callData: ApiCallData) => Promise<Response | ApiResponse<any>>,
            }
            
            export type ApiInterceptor = (param: ApiCallData) => ApiCallData | Promise<ApiCallData>
        """
                .trimIndent() + "\n"
        val referenceTypes = typeDefinitions.values.filterIsInstance<TsGenerator.ReferenceType>()
        return internalTypes + referenceTypes.joinToString("\n\n") { "export ${it.content()}" }
    }

    override fun generateClient(): List<ClientPart> {
        val clientTypes = getClientTypes()
        val clientRequests = getClientRequests()
        return listOf(
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
