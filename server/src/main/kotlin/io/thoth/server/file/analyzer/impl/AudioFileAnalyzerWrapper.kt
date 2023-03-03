package io.thoth.server.file.analyzer.impl

import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import io.thoth.server.file.analyzer.AudioFileAnalyzer
import io.thoth.server.file.analyzer.AudioFileAnalyzerWrapper
import io.thoth.server.file.tagger.ReadonlyFileTaggerImpl
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute
import mu.KotlinLogging.logger

class AudioFileAnalyzerWrapperImpl(private val analyzers: List<AudioFileAnalyzer>) : AudioFileAnalyzerWrapper {
    private val log = logger {}

    override suspend fun analyze(path: Path, attrs: BasicFileAttributes): AudioFileAnalysisResult? {
        val tags = ReadonlyFileTaggerImpl(path)
        for (analyzer in analyzers) {
            try {
                val result = analyzer.analyze(path, attrs, tags)
                if (result != null) return result
            } catch (e: Exception) {
                log.warn(e) { "Could not analyze file ${path.absolute()}" }
            }
        }
        return null
    }
}
