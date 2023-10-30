package io.thoth.openapi.client.common

import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.Summary
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

    protected fun getRouteName(route: OpenApiRoute): String? {
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
}
