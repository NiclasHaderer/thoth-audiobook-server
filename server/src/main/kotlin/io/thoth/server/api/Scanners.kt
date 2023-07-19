package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.models.FileScanner
import io.thoth.openapi.ktor.get
import io.thoth.server.file.analyzer.AudioFileAnalyzers
import org.koin.ktor.ext.inject

fun Routing.scannerRouting() {
    val scanners by inject<AudioFileAnalyzers>()
    get<Api.Scanners, List<FileScanner>> { scanners.map { FileScanner(it.name) } }
}
