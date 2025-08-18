package io.thoth.openapi.ktor

import io.ktor.server.routing.RoutingContext

interface BeforeBodyParsing {
    suspend fun RoutingContext.beforeBodyParsing()
}

interface AfterBodyParsing {
    suspend fun RoutingContext.afterBodyParsing()
}

interface ValidateObject {
    suspend fun RoutingContext.validateBody()
}

interface AfterResponse {
    suspend fun RoutingContext.afterResponse()
}
