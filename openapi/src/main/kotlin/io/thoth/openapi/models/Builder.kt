package io.thoth.openapi.models

import io.swagger.v3.oas.models.OpenAPI

internal fun openApi(configure: OpenAPIContext.() -> Unit): OpenAPI {
    val api = OpenAPI()
    val context = generalOpenApiContext(api)
    context.apply(configure)
    return api
}

internal fun generalOpenApiContext(api: OpenAPI? = null): OpenAPIContext {
    return OpenAPIContext(api ?: OpenAPI())
}
