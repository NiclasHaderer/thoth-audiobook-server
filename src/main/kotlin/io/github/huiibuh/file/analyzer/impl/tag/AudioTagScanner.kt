package io.github.huiibuh.file.analyzer.impl.tag

import io.github.huiibuh.file.analyzer.AnalysisResult
import io.github.huiibuh.file.analyzer.AudioFileAnalysisValueImpl
import io.github.huiibuh.file.analyzer.AudioFileAnalyzer
import io.github.huiibuh.file.tagger.ReadonlyFileTaggerImpl
import io.github.huiibuh.settings.Settings
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class AudioTagScanner(settings: Settings) : AudioFileAnalyzer(settings) {
    override suspend fun analyze(path: Path, attrs: BasicFileAttributes): AnalysisResult {
        val tagger = ReadonlyFileTaggerImpl(path)
        if (tagger.author == null || tagger.book == null) return AnalysisResult(false, null)
        return AnalysisResult(true, AudioFileAnalysisValueImpl(
            title = tagger.title,
            author = tagger.author!!,
            book = tagger.book!!,
            description = tagger.description,
            year = tagger.year,
            language = tagger.language,
            trackNr = tagger.trackNr,
            narrator = tagger.narrator,
            series = tagger.series,
            seriesIndex = tagger.seriesIndex,
            cover = tagger.cover,
            duration = tagger.duration,
            path = tagger.path,
            lastModified = tagger.lastModified
        ))
    }
}
