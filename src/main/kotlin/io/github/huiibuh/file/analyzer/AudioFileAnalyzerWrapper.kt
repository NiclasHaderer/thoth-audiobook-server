package io.github.huiibuh.file.analyzer

import io.github.huiibuh.file.tagger.ReadonlyFileTaggerImpl
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class AudioFileAnalyzerWrapperImpl(private val analyzers: List<AudioFileAnalyzer>) :
    AudioFileAnalyzerWrapper {
    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun analyze(path: Path, attrs: BasicFileAttributes): AudioFileAnalysisResult? {
        val tags = ReadonlyFileTaggerImpl(path)
        for (analyzer in analyzers) {
            try {
                val result = analyzer.analyze(path, attrs, tags)
                if (result != null) return result
            } catch (e: Exception) {
                log.warn(e.message)
            }
        }
        return null
    }
}
