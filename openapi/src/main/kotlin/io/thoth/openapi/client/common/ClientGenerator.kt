package io.thoth.openapi.client.common

import io.thoth.openapi.ktor.OpenApiRoute
import java.nio.file.Path

abstract class ClientGenerator(private val dist: Path) {
    protected abstract val routes: List<OpenApiRoute>

    protected abstract fun generateClient(): List<ClientPart>

    fun safeClient() {
        val client = generateClient()
        client.forEach {
            val destination = dist.resolve(it.path).toFile()
            destination.parentFile.mkdirs()
            destination.writeText(it.content)
        }
    }
}
