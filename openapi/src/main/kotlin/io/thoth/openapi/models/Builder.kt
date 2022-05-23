package io.thoth.openapi.models

import io.swagger.v3.oas.models.OpenAPI

internal fun generalOpenApiContext(api: OpenAPI? = null): OpenAPIContext {
    return OpenAPIContext(api ?: OpenAPI())
}
