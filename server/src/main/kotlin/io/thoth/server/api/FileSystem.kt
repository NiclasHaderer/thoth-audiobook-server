package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import java.io.File

fun Routing.fileSystemRouting() {
    get<Api.FileSystem, List<FileSystemItem>> { (path, showHidden) ->
        val directory = File(path)

        if (!directory.exists() || !directory.isDirectory) throw ErrorResponse.notFound("Directory", path)

        val children =
            directory.listFiles()
                ?: throw ErrorResponse.forbidden("list", "Directory $path")

        children
            .filter { it.isDirectory && (showHidden || !it.isHidden) && !it.listFiles().isNullOrEmpty() }
            .map { FileSystemItem(name = it.name, path = it.path, parent = it.parentFile?.path) }
            .sortedBy { it.name }
    }
}
