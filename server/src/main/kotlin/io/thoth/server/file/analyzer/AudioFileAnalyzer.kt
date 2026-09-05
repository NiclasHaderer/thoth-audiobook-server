package io.thoth.server.file.analyzer

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.server.file.analyzer.impl.AudioFileAnalyzerWrapper
import io.thoth.server.file.tagger.ReadonlyFileTagger
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

    private val byName by lazy { associateBy { it.name } }

    fun forNames(
        names: List<String>,
        combineFields: Boolean = false,
    ): AudioFileAnalyzerWrapper {
        val libAnalyzer = names.distinct().mapNotNull { byName[it] }

        if (libAnalyzer.isEmpty()) {
            log.error {
                "Library does not reference any available scanners" +
                    " (available scanners: ${map { it.name }})" +
                    " (library scanners: $names)"
            }
        }

        return AudioFileAnalyzerWrapper(libAnalyzer, combineFields)
    }
}
