package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.client.common.mappedKtReference
import io.thoth.openapi.ktor.OpenApiRoute
import mu.KotlinLogging.logger

class KtClientFunction(
    val getRouteName: (OpenApiRoute) -> String?,
    val route: OpenApiRoute,
    val clientImports: MutableSet<String>,
    val typeDefinitions: MutableMap<String, KtTypeGenerator.KtReferenceType>,
    val typeProviders: TypeGenerator.Provider<KtTypeGenerator.KtType, KtTypeGenerator.KtDataType, KtTypeGenerator>,
) {

    companion object {
        private val log = logger {}
    }

    val content = run { generateContent() }

    private fun getParameters(route: OpenApiRoute) = buildList {
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

        // Headers
        add("headers: Headers = Headers.Empty, ")

        val (responseBody, _) = typeProviders.generateTypes(route.responseBodyType)
        val (requestBody, _) = typeProviders.generateTypes(route.requestBodyType)
        // Hooks to modify the request
        add("onBeforeRequest: OnBeforeRequest<${requestBody.reference()}> = { _, _ -> }, ")
        add("onAfterRequest: OnAfterRequest<${requestBody.reference()}, ${responseBody.reference()}> = { _, _ -> }")
    }

    private fun generateContent(): String? {
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
            append("    open suspend fun ${routeName}(\n")
            getParameters(route).forEach { append("        $it\n") }
            append("    ): OpenApiHttpResponse<${responseBody.reference()}> {\n")
            append("        return makeRequest(\n")
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
