package io.thoth.server.file.analyzer.impl

import io.thoth.common.extensions.*
 import io.thoth.config.public.PublicConfig
import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import io.thoth.server.file.analyzer.AudioFileAnalysisResultImpl
import io.thoth.server.file.analyzer.AudioFileAnalyzer
import io.thoth.server.file.tagger.ReadonlyFileTagger
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class AudioFolderScanner(publicConfig: PublicConfig) : AudioFileAnalyzer(publicConfig) {
    private val bookPrefixes = listOf(
        "^((Book|Volume|Vol) ?)?\\d\\d? ?[.\\-: ]+ ?".toRegex(),
        "^\\d\\d? - ".toRegex()
    )

    override suspend fun analyze(
        path: Path, attrs: BasicFileAttributes, tags: ReadonlyFileTagger
    ): AudioFileAnalysisResult? {
        val cleanPath = path.replaceParts(publicConfig.audioFileLocations)
        val parentCount = cleanPath.countParents()
        if (parentCount != 2 && parentCount != 3) return null

        return this.getInformation(cleanPath, parentCount, tags)
    }

    private fun getInformation(path: Path, parentCount: Int, tags: ReadonlyFileTagger): AudioFileAnalysisResult {
        val book = path.parentName().replaceAll(bookPrefixes, "")
        val author = if (parentCount == 2) path.grandParentName() else path.grandGrandParentName()
        val series = if (parentCount == 2) null else path.grandParentName()
        return AudioFileAnalysisResultImpl( // Uses the filename as fallback
            title = tags.title,
            author = tags.author ?: author,
            book = tags.book ?: book,
            series = tags.series ?: series,
            description = tags.description,
            date = tags.date,
            language = tags.language,
            trackNr = tags.trackNr,
            narrator = tags.narrator,
            seriesIndex = tags.seriesIndex,
            cover = tags.cover,
            duration = tags.duration,
            path = tags.path,
            lastModified = tags.lastModified,
        )
    }
}
