package io.thoth.server.file.analyzer.impl

import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import io.thoth.server.file.analyzer.AudioFileAnalyzerWrapper
import io.thoth.server.file.analyzer.AudioFileAnalyzers
import io.thoth.server.file.tagger.ReadonlyFileTaggerImpl
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute
import mu.KotlinLogging.logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AudioFileAnalyzerWrapperImpl() : AudioFileAnalyzerWrapper, KoinComponent {
    private val analyzers by inject<AudioFileAnalyzers>()
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
