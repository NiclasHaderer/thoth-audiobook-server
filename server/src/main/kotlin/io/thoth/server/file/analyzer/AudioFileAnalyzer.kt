package io.thoth.server.file.analyzer

import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.file.analyzer.impl.AudioFileAnalyzerWrapper
import io.thoth.server.file.tagger.ReadonlyFileTagger
import mu.KotlinLogging.logger
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

interface AudioFileAnalyzer {
    val name: String

    fun analyze(
        filePath: Path,
        attrs: BasicFileAttributes,
        tags: ReadonlyFileTagger,
        libraryPath: Path,
    ): AudioFileAnalysisResult?
}

class AudioFileAnalyzers(
    private val items: List<AudioFileAnalyzer>,
) : List<AudioFileAnalyzer> by items {
    private val log = logger {}

    fun forLibrary(library: LibraryEntity): AudioFileAnalyzerWrapper {
        val libAnalyzer =
            filter { analyzer -> analyzer.name in library.fileScanners.map { libScanner -> libScanner.name } }

        if (libAnalyzer.isEmpty()) {
            log.error {
                "Library does not reference any available scanners"
                " (available scanners: ${map { it.name }})"
                " (library scanners: ${library.fileScanners.map { it.name }})"
            }
        }

        return AudioFileAnalyzerWrapper(libAnalyzer)
    }
}
