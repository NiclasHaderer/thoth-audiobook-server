package io.thoth.openapi.client.common

import io.thoth.openapi.ktor.OpenApiRoute


abstract class ClientGenerator {
    protected abstract val routes: List<OpenApiRoute>

    protected abstract fun generateClient(): List<ClientPart>

    fun safeClient() {
        val client = generateClient()
        client.forEach {
            it.path.toFile().parentFile.mkdirs()
            it.path.toFile().writeText(it.content)
        }
    }
}
