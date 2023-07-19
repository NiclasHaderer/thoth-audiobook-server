package io.thoth.openapi.ktor

interface BeforeBodyParsing {
    suspend fun RouteHandler.beforeBodyParsing()
}

interface AfterBodyParsing {
    suspend fun RouteHandler.afterBodyParsing()
}

interface AfterResponse {
    suspend fun RouteHandler.afterResponse()
}
