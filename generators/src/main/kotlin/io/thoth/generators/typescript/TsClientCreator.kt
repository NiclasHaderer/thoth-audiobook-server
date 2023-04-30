package io.thoth.generators.typescript

import io.thoth.generators.openapi.OpenApiRoute
import io.thoth.generators.openapi.Summary
import io.thoth.generators.typescript.types.TsGenerator
import io.thoth.generators.typescript.types.generateTypes
import java.io.File

class TsClientCreator(
    private val routes: List<OpenApiRoute>,
    private val typesFile: File,
    private val clientFile: File,
) {
    private val typeDefinitions = mutableMapOf<String, TsGenerator.Type>()
    private val clientFunctions = mutableListOf<String>()

    init {
        generateApiClient()
    }

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
        const __request = <T>(
                route: string, 
                method: string, 
                bodyParseMethod: "text" | "json" | "blob", 
                _headers: HeadersInit,
                body?: object, 
                interceptors?: ApiInterceptor[]
            ): Promise<ApiResponse<T>> => {
            
            const headers = new Headers(_headers);
            if(!headers.has("Content-Type")) {
                headers.set("Content-Type", "application/json");
            }
            let apiCallData: ApiCallData = {
                route, 
                method, 
                body, 
                headers, 
                bodySerializer: (body?: object) => body ? JSON.stringify(body) : undefined,
                executor: (route: string, method: string, headers: Headers, body: string | undefined) => fetch(route, {method, headers, body})
            };
            if (interceptors) {
                for (const interceptor of interceptors) {
                    apiCallData = interceptor(apiCallData);
                }
            }
                    
            return apiCallData.executor(apiCallData.route, apiCallData.method, apiCallData.headers, apiCallData.bodySerializer(apiCallData.body))
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

    private fun getRouteName(route: OpenApiRoute): String? {
        val summary = route.requestParamsType.findAnnotations<Summary>().firstOrNull { it.method == route.method.value }
        val summaryString = summary?.summary ?: return null
        return summaryString
            .split(" ")
            .mapIndexed { index, word ->
                word.replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase()
                    } else if (index == 0) {
                        it.lowercase()
                    } else {
                        it.toString()
                    }
                }
            }
            .joinToString("")
    }

    private fun getParameters(route: OpenApiRoute): String {
        val pathParams =
            route.pathParameters.joinToString(", ") { (param) ->
                val (actual, all) = generateTypes(param.type)
                typeDefinitions.putAll(all.associateBy { it.name })
                "${param.name}: ${actual.reference()}"
            }
        val requiredQueryParams =
            route.queryParameters
                .filter { !it.first.optional }
                .joinToString(", ") { (param) ->
                    val (actual, all) = generateTypes(param.type)
                    typeDefinitions.putAll(all.associateBy { it.name })
                    "${param.name}${if (param.optional) "?" else ""}: ${actual.reference()}"
                }
        val bodyParam =
            if (route.requestBodyType.clazz != Unit::class) {
                val (actual, all) = generateTypes(route.requestBodyType)
                typeDefinitions.putAll(all.associateBy { it.name })
                "body: ${actual.reference()}"
            } else ""

        val optionalQueryParams =
            route.queryParameters
                .filter { it.first.optional }
                .joinToString(", ") { (param) ->
                    val (actual, all) = generateTypes(param.type)
                    typeDefinitions.putAll(all.associateBy { it.name })
                    "${param.name}?: ${actual.reference()}"
                }

        val customHeaders =
            listOf(pathParams, requiredQueryParams, bodyParam, optionalQueryParams)
                .filter { it.isNotBlank() }
                .joinToString(", ")

        return if (customHeaders.isNotBlank()) {
            "$customHeaders, headers: HeadersInit = {}, interceptors?: ApiInterceptor[]"
        } else {
            "headers: HeadersInit = {}, interceptors?: ApiInterceptor[]"
        }
    }

    private fun createURL(route: OpenApiRoute): String {
        // Add them to the URLSearchParams
        val finalPath = route.fullPath.replace("{", "\${")
        return """
        ${
            if (route.queryParameters.isEmpty()) {
                "const __finalUrl = `$finalPath`;"
            } else {
                "const __finalUrl = __createUrl(`$finalPath`, {${route.queryParameters.joinToString(", ") { it.first.name }}});"
            }
        }
    """
            .trimIndent()
    }

    private fun generateApiClient() {
        routes.forEach { route ->
            val routeName = getRouteName(route)
            if (routeName == null) {
                println("Route ${route.method}:${route.fullPath} has no summary")
                return@forEach
            }
            val responseBody = generateTypes(route.responseBodyType).first
            val function =
                """
            $routeName(${getParameters(route)}): Promise<ApiResponse<${responseBody.reference()}>> {
                ${createURL(route)}
                return __request(__finalUrl, "${route.method.value}", "${responseBody.parser.methodName}", headers, ${if (route.requestBodyType.clazz != Unit::class) "body" else "undefined"}, interceptors);
            }
        """
                    .trimIndent()
            clientFunctions.add(function)

            val responseInterfaces = generateTypes(route.responseBodyType)
            typeDefinitions.putAll(responseInterfaces.second.associateBy { it.name })
        }
    }

    private fun createTypeImports(): String {
        return "import type {${
            typeDefinitions.values
                .asSequence()
                .filter { it.inlineMode == TsGenerator.InsertionMode.REFERENCE }
                .map {
                    // Replace the generic <> with nothing to not break the import
                    it.name.replace("<.*>".toRegex(), "")
                }.distinct()
                .sorted()
                .joinToString(", ") + ", ApiResponse, ApiInterceptor, ApiCallData"
        }} from \"./${this.typesFile.nameWithoutExtension}\";\n"
    }

    public fun getClientFunctions(): String {
        return createTypeImports() +
            createUrlCreator() +
            createRequestMaker() +
            "export const api = {\n" +
            clientFunctions.joinToString(
                ",\n",
            ) {
                "  $it"
            } +
            "} as const;"
    }

    public fun getClientTypes(): String {
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
                route: string,
                method: string,
                body?: object,
                headers: Headers,
                bodySerializer: (body?: object) => string | undefined,
                executor: (route: string, method: string, headers: Headers, body: string | undefined) => Promise<Response | ApiResponse<any>> 
            }
            
            export type ApiInterceptor = (param: ApiCallData) => ApiCallData
        """
                .trimIndent() + "\n"
        val referenceTypes = typeDefinitions.values.filter { it.inlineMode == TsGenerator.InsertionMode.REFERENCE }
        return internalTypes + referenceTypes.joinToString("\n\n") { "export ${it.content}" }
    }

    fun saveTypes() {
        typesFile.writeText(getClientTypes())
    }

    fun saveClient() {
        clientFile.writeText(getClientFunctions())
    }
}
