package io.github.huiibuh.file.analyzer

import io.github.huiibuh.settings.Settings
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class AudioFileAnalyzerWrapper(settings: Settings, private val analyzers: List<AudioFileAnalyzer>) :
    AudioFileAnalyzer(settings) {
    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun analyze(path: Path, attrs: BasicFileAttributes): AnalysisResult {
        for (analyzer in analyzers) {
            try {
                val result = analyzer.analyze(path, attrs)
                if (result.success) return result
            } catch (e: Exception) {
                log.warn(e.message)
            }
        }
        return AnalysisResult(false, null)
    }
}
