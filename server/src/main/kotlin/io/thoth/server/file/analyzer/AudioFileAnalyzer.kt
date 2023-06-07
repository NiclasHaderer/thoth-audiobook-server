package io.thoth.server.file.analyzer

import io.thoth.server.file.tagger.ReadonlyFileTagger
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

interface AudioFileAnalyzer {
    val name: String
    fun analyze(
        filePath: Path,
        attrs: BasicFileAttributes,
        tags: ReadonlyFileTagger,
        libraryPath: Path
    ): AudioFileAnalysisResult?
}

class AudioFileAnalyzers(private val items: List<AudioFileAnalyzer>) : List<AudioFileAnalyzer> by items
