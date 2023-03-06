package io.thoth.server.file.analyzer.impl

import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import io.thoth.server.file.analyzer.AudioFileAnalysisResultImpl
import io.thoth.server.file.analyzer.AudioFileAnalyzer
import io.thoth.server.file.tagger.ReadonlyFileTagger
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class AudioTagScanner : AudioFileAnalyzer {
    override fun analyze(
        filePath: Path,
        attrs: BasicFileAttributes,
        tags: ReadonlyFileTagger,
        libraryPath: Path
    ): AudioFileAnalysisResult? {
        if (tags.author == null || tags.book == null) return null
        return AudioFileAnalysisResultImpl(
            title = tags.title,
            author = tags.author!!,
            book = tags.book!!,
            description = tags.description,
            date = tags.date,
            language = tags.language,
            trackNr = tags.trackNr,
            narrator = tags.narrator,
            series = tags.series,
            seriesIndex = tags.seriesIndex,
            cover = tags.cover,
            duration = tags.duration,
            path = tags.path,
            lastModified = tags.lastModified,
        )
    }
}
