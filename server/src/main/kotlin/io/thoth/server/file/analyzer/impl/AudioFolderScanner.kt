package io.thoth.server.file.analyzer.impl

import io.thoth.server.common.extensions.countParents
import io.thoth.server.common.extensions.grandGrandParentName
import io.thoth.server.common.extensions.grandParentName
import io.thoth.server.common.extensions.parentName
import io.thoth.server.common.extensions.replaceAll
import io.thoth.server.common.extensions.replacePart
import io.thoth.server.file.analyzer.AudioFileAnalysisResult
import io.thoth.server.file.analyzer.AudioFileAnalysisResultImpl
import io.thoth.server.file.analyzer.AudioFileAnalyzer
import io.thoth.server.file.tagger.ReadonlyFileTagger
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolutePathString

class AudioFolderScanner : AudioFileAnalyzer {
    override val name = "AudioFolderScanner"
    private val bookPrefixes = listOf("^((Book|Volume|Vol) ?)?\\d\\d? ?[.\\-: ]+ ?".toRegex(), "^\\d\\d? - ".toRegex())

    override fun analyze(
        filePath: Path,
        attrs: BasicFileAttributes,
        tags: ReadonlyFileTagger,
        libraryPath: Path
    ): AudioFileAnalysisResult? {
        val cleanPath = filePath.replacePart(libraryPath.absolutePathString())
        val parentCount = cleanPath.countParents()
        if (parentCount != 2 && parentCount != 3) return null

        return this.getInformation(cleanPath, parentCount, tags)
    }

    private fun getInformation(path: Path, parentCount: Int, tags: ReadonlyFileTagger): AudioFileAnalysisResult {
        val book = path.parentName().replaceAll(bookPrefixes, "")
        val author = if (parentCount == 2) path.grandParentName() else path.grandGrandParentName()
        val series = if (parentCount == 2) null else path.grandParentName()
        return AudioFileAnalysisResultImpl(
            // Uses the filename as fallback
            title = tags.title,
            authors = tags.authors ?: listOf(author),
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
