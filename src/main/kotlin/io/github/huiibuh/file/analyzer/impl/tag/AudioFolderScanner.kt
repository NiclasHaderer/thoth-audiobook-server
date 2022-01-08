package io.github.huiibuh.file.analyzer.impl.tag

import io.github.huiibuh.extensions.countParents
import io.github.huiibuh.extensions.grandGrandParentName
import io.github.huiibuh.extensions.grandParentName
import io.github.huiibuh.extensions.parentName
import io.github.huiibuh.extensions.replacePart
import io.github.huiibuh.file.analyzer.AnalysisResult
import io.github.huiibuh.file.analyzer.AudioFileAnalysisValue
import io.github.huiibuh.file.analyzer.AudioFileAnalysisValueImpl
import io.github.huiibuh.file.analyzer.AudioFileAnalyzer
import io.github.huiibuh.file.tagger.ReadonlyFileTaggerImpl
import io.github.huiibuh.settings.Settings
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class AudioFolderScanner(settings: Settings) : AudioFileAnalyzer(settings) {
    override suspend fun analyze(path: Path, attrs: BasicFileAttributes): AnalysisResult {
        val cleanPath = path.replacePart(settings.audioFileLocation)
        val parentCount = cleanPath.countParents()
        if (parentCount != 2 && parentCount != 3) return AnalysisResult(false, null)

        val result = this.getInformation(cleanPath, parentCount)
        return AnalysisResult(false, null)
    }

    private fun getInformation(path: Path, parentCount: Int): AudioFileAnalysisValue {
        val book = path.parentName()
        val author = if (parentCount == 2) path.grandParentName() else path.grandGrandParentName()
        val series = if (parentCount == 2) null else path.grandParentName()
        val tagger = ReadonlyFileTaggerImpl(path)
        return AudioFileAnalysisValueImpl(
            // Uses the filename as fallback
            title = tagger.title,
            author = tagger.author ?: author,
            book = tagger.book ?: book,
            series = tagger.series ?: series,
            description = tagger.description,
            year = tagger.year,
            language = tagger.language,
            trackNr = tagger.trackNr,
            narrator = tagger.narrator,
            seriesIndex = tagger.seriesIndex,
            cover = tagger.cover,
            duration = tagger.duration,
            path = tagger.path,
            lastModified = tagger.lastModified
        )
    }
}
