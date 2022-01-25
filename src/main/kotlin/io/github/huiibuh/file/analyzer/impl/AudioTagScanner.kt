package io.github.huiibuh.file.analyzer.impl

import io.github.huiibuh.file.analyzer.AudioFileAnalysisResult
import io.github.huiibuh.file.analyzer.AudioFileAnalysisResultImpl
import io.github.huiibuh.file.analyzer.AudioFileAnalyzer
import io.github.huiibuh.file.tagger.ReadonlyFileTagger
import io.github.huiibuh.settings.Settings
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class AudioTagScanner(settings: Settings) : AudioFileAnalyzer(settings) {
    override suspend fun analyze(
        path: Path,
        attrs: BasicFileAttributes,
        tags: ReadonlyFileTagger
    ): AudioFileAnalysisResult? {
        if (tags.author == null || tags.book == null) return null
        return AudioFileAnalysisResultImpl(
            title = tags.title,
            author = tags.author!!,
            book = tags.book!!,
            description = tags.description,
            year = tags.year,
            language = tags.language,
            trackNr = tags.trackNr,
            narrator = tags.narrator,
            series = tags.series,
            seriesIndex = tags.seriesIndex,
            cover = tags.cover,
            duration = tags.duration,
            path = tags.path,
            lastModified = tags.lastModified,
            providerId = tags.providerId
        )
    }
}
