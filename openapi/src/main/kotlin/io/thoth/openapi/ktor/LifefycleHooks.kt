package io.thoth.openapi.ktor

interface BeforeBodyParsing {
    suspend fun RouteHandler.beforeBodyParsing()
}

interface AfterBodyParsing {
    suspend fun RouteHandler.afterBodyParsing()
}

interface ValidateObject {
    suspend fun RouteHandler.validateBody()
}

interface AfterResponse {
    suspend fun RouteHandler.afterResponse()
}
