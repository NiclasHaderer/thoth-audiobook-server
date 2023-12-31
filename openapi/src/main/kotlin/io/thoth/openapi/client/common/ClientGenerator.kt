package io.thoth.openapi.client.common

import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.Summary
import java.io.File
import java.nio.file.Path

abstract class ClientGenerator(
    private val dist: Path,
    fileWriter: ((File, String) -> Unit)?,
    private val cleanDistPackage: Boolean
) {

    private val fileWriter =
        fileWriter
            ?: { file, content ->
                file.parentFile.mkdirs()
                file.writeText(content)
            }

    protected abstract val routes: List<OpenApiRoute>

    protected abstract fun generateClient(): List<ClientPart>

    fun safeClient() {
        val client = generateClient()
        if (cleanDistPackage) {
            dist.toFile().deleteRecursively()
        }
        client.forEach {
            val destination = dist.resolve(it.path).toFile()
            fileWriter(destination, it.content)
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
            .replace(Regex("[^A-Za-z0-9]"), "")
    }
}
