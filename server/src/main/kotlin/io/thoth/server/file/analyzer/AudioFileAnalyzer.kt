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

interface AudioFileAnalyzerWrapper {
    fun analyze(filePath: Path, attrs: BasicFileAttributes, libraryPath: Path): AudioFileAnalysisResult?
}
