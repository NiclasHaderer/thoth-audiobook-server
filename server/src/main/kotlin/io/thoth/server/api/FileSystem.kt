package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.generators.openapi.get
import java.io.File

fun Routing.fileSystemRouting() {
    get<Api.FileSystem, List<FileSystemItem>> { (path, showHidden) ->
        val directory = File(path)

        if (!directory.exists() || !directory.isDirectory) throw ErrorResponse.notFound("Directory", path)

        directory
            .listFiles()!!
            .filter { it.isDirectory }
            .filter { if (showHidden) true else !it.isHidden }
            .filter {
                val hasChildren = it.listFiles()?.isNotEmpty() ?: false
                hasChildren
            }
            .map {
                FileSystemItem(
                    name = it.name,
                    path = it.path,
                    parent = it.parentFile?.path,
                )
            }
            .sortedBy { it.name }
    }
}
