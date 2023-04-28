package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.generators.openapi.get
import io.thoth.models.FileScanner
import io.thoth.server.di.AudioFileAnalyzers
import org.koin.ktor.ext.inject

fun Routing.scannerRouting() {
    val scanners by inject<AudioFileAnalyzers>()
    scanners.forEach { println(it.name) }
    get<Api.Scanners, List<FileScanner>> { scanners.map { FileScanner(it.name) } }
}
