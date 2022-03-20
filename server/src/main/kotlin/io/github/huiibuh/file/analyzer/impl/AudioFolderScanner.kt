package io.github.huiibuh.file.analyzer.impl

import io.github.huiibuh.extensions.*
import io.github.huiibuh.file.analyzer.AudioFileAnalysisResult
import io.github.huiibuh.file.analyzer.AudioFileAnalysisResultImpl
import io.github.huiibuh.file.analyzer.AudioFileAnalyzer
import io.github.huiibuh.file.tagger.ReadonlyFileTagger
import io.github.huiibuh.settings.Settings
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class AudioFolderScanner(settings: Settings) : AudioFileAnalyzer(settings) {
    override suspend fun analyze(
        path: Path,
        attrs: BasicFileAttributes,
        tags: ReadonlyFileTagger
    ): AudioFileAnalysisResult? {
        val cleanPath = path.replacePart(settings.audioFileLocation)
        val parentCount = cleanPath.countParents()
        if (parentCount != 2 && parentCount != 3) return null

        return this.getInformation(cleanPath, parentCount, tags)
    }

    private fun getInformation(path: Path, parentCount: Int, tags: ReadonlyFileTagger): AudioFileAnalysisResult {
        val book = path.parentName()
        val author = if (parentCount == 2) path.grandParentName() else path.grandGrandParentName()
        val series = if (parentCount == 2) null else path.grandParentName()
        return AudioFileAnalysisResultImpl(
            // Uses the filename as fallback
            title = tags.title,
            author = tags.author ?: author,
            book = tags.book ?: book,
            series = tags.series ?: series,
            description = tags.description,
            year = tags.year,
            language = tags.language,
            trackNr = tags.trackNr,
            narrator = tags.narrator,
            seriesIndex = tags.seriesIndex,
            cover = tags.cover,
            duration = tags.duration,
            path = tags.path,
            lastModified = tags.lastModified,
            providerId = tags.providerId
        )
    }
}
