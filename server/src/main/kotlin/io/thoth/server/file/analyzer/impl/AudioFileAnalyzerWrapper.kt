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

    override fun analyze(filePath: Path, attrs: BasicFileAttributes, libraryPath: Path): AudioFileAnalysisResult? {
        val tags = ReadonlyFileTaggerImpl(filePath)
        for (analyzer in analyzers) {
            try {
                val result = analyzer.analyze(filePath, attrs, tags, libraryPath)
                if (result != null) return result
            } catch (e: Exception) {
                log.warn(e) { "Could not analyze file ${filePath.absolute()}" }
            }
        }
        return null
    }
}
