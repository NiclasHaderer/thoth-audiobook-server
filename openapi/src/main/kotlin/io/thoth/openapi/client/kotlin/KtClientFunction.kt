package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.client.common.mappedKtReference
import io.thoth.openapi.ktor.OpenApiRoute
import mu.KotlinLogging.logger

class KtClientFunction(
    private val getRouteName: (OpenApiRoute) -> String?,
    private val route: OpenApiRoute,
    private val clientImports: MutableSet<String>,
    private val typeDefinitions: MutableMap<String, KtTypeGenerator.KtReferenceType>,
    private val typeProviders:
        TypeGenerator.Provider<KtTypeGenerator.KtType, KtTypeGenerator.KtDataType, KtTypeGenerator>,
    private val errorHandling: KtErrorHandling,
) {

    companion object {
        private val log = logger {}
    }

    val contentImpl = run { generateContent(true) }
    val content = run { generateContent(false) }

    private fun getParameters(route: OpenApiRoute, impl: Boolean) = buildList {
        // Path parameters
        (route.queryParameters + route.pathParameters).forEach { (param) ->
            val (actual, all) = typeProviders.generateTypes(param.type)
            clientImports.addAll(actual.imports())
            typeDefinitions.putAll(all.mappedKtReference())
            add("${param.name}: ${actual.reference()}${if (param.optional) "?" else ""}, ")
        }

        // Body
        if (route.requestBodyType.clazz != Unit::class) {
            val (actual, all) = typeProviders.generateTypes(route.requestBodyType)
            clientImports.addAll(actual.imports())
            typeDefinitions.putAll(all.mappedKtReference())
            add("body: ${actual.reference()}, ")
        }

        fun withIfNotImpl(str: String) = if (impl) "" else str

        // Headers
        add("headers: Headers${withIfNotImpl("= Headers.Empty")},")

        val (responseBody, _) = typeProviders.generateTypes(route.responseBodyType)
        val (requestBody, _) = typeProviders.generateTypes(route.requestBodyType)
        // Hooks to modify the request
        add("onBeforeRequest: OnBeforeRequest<${requestBody.reference()}>${withIfNotImpl(" = { _, _ -> }")}, ")
        add(
            "onAfterRequest: OnAfterRequest<${requestBody.reference()}, ${responseBody.reference()}>${withIfNotImpl(" = { _, _ -> }")}"
        )
    }

    private fun generateContent(impl: Boolean): String? {
        val routeName = getRouteName(route)
        if (routeName == null) {
            log.warn("Route ${route.method}:${route.fullPath} has no summary")
            return null
        }

        val (responseBody, all) = typeProviders.generateTypes(route.responseBodyType)
        val (requestBody, _) = typeProviders.generateTypes(route.requestBodyType)
        clientImports.addAll(responseBody.imports())
        clientImports.addAll(requestBody.imports())
        typeDefinitions.putAll(all.mappedKtReference())
        return buildString {
            append("    ${if (impl) "override" else ""} suspend fun ${routeName}(\n")
            getParameters(route, impl).forEach { append("        $it\n") }
            val functionRunner =
                when (errorHandling) {
                    KtErrorHandling.Result ->
                        "= runCatching" to "Result<OpenApiHttpResponse<${responseBody.reference()}>>"
                    KtErrorHandling.Exception -> "= run" to "OpenApiHttpResponse<${responseBody.reference()}>"
                    KtErrorHandling.Either ->
                        "= wrapInEither" to "Either<OpenApiHttpResponse<${responseBody.reference()}>, ApiError>"
                }
            append("    ): ${functionRunner.second}")
            if (!impl) {
                append("\n")
                return@buildString
            }
            append(" ${functionRunner.first} {\n")
            append("        makeRequest(\n")
            append("            RequestMetadata(\n")
            append("                path = \"${route.fullPath}\",\n")
            append("                method = HttpMethod(\"${route.method.value}\"),\n")
            append("                headers = headers,\n")
            append("                body = ${if (route.requestBodyType.clazz == Unit::class) "Unit" else "body"},\n")
            append("                shouldLogin = ${route.secured != null},\n")
            append("                securitySchema = \"${route.secured?.name}\",\n")
            append("            ),\n")
            append("            typeInfo<${requestBody.referenceImpl()}>(),\n")
            append("            typeInfo<${responseBody.referenceImpl()}>(),\n")
            append("            onBeforeRequest=onBeforeRequest,\n")
            append("            onAfterRequest=onAfterRequest\n")
            append("        )")
            append("\n    }")
        }
    }
}
