package io.thoth.server.file.analyzer

import io.thoth.server.file.tagger.ReadonlyFileTagger
import io.thoth.server.settings.Settings
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

abstract class AudioFileAnalyzer(
    protected val settings: Settings,
) {
    abstract suspend fun analyze(
        path: Path,
        attrs: BasicFileAttributes,
        tags: ReadonlyFileTagger
    ): AudioFileAnalysisResult?
}

interface AudioFileAnalyzerWrapper {
    suspend fun analyze(path: Path, attrs: BasicFileAttributes): AudioFileAnalysisResult?
}
