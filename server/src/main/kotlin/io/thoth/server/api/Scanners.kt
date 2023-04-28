package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.generators.openapi.get
import io.thoth.models.FileScanner
import io.thoth.server.file.analyzer.AudioFileAnalyzer
import org.koin.ktor.ext.inject


fun Routing.scannerRouting() {
    val scanners by inject<List<AudioFileAnalyzer>>()
    get<Api.Scanners, List<FileScanner>> {
        scanners.map { FileScanner(it.name) }
    }
}
