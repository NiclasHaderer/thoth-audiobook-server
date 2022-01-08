package io.github.huiibuh.file.analyzer

import io.github.huiibuh.settings.Settings
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

abstract class AudioFileAnalyzer(
    protected val settings: Settings,
) {
    abstract suspend fun analyze(path: Path, attrs: BasicFileAttributes): AnalysisResult
}
