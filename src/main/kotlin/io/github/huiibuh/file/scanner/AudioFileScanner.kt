package io.github.huiibuh.file.scanner

import io.github.huiibuh.extensions.classLogger
import io.github.huiibuh.file.analyzer.AudioFileAnalysisResult
import io.github.huiibuh.file.analyzer.AudioFileAnalyzerWrapper
import io.github.huiibuh.settings.Settings
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString


class AudioFileScanner(
    private val pathHandler: AudioFileAnalyzerWrapper,
    private val removeSubtree: (Path) -> Unit,
    private val shouldUpdateFile: (Path) -> Boolean,
    private val addOrUpdate: (file: Path, attrs: BasicFileAttributes, result: AudioFileAnalysisResult) -> Unit,
) : FileVisitor<Path>, KoinComponent {
    private val settings: Settings by inject()
    private val log = classLogger()

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
        val ignoreFile =
            Paths.get("${dir.absolutePathString()}${FileSystems.getDefault().separator}${settings.ignoreFile}")
        return if (!Files.exists(ignoreFile)) {
            FileVisitResult.CONTINUE
        } else {
            removeSubtree(dir.absolute().normalize())
            log.debug("Ignoring directory ${dir.absolute().normalize()}")
            FileVisitResult.SKIP_SUBTREE
        }
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (file.isAudioFile() && shouldUpdateFile(file.normalize())) {
            val value = runBlocking { pathHandler.analyze(file, attrs) }
            if (value != null) {
                addOrUpdate(file.absolute().normalize(), attrs, value)
            } else {
                log.debug("Ignoring file ${file.absolute().absolute().normalize()}")
            }
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path, exc: IOException?) = FileVisitResult.CONTINUE

    override fun postVisitDirectory(dir: Path, exc: IOException?) = FileVisitResult.CONTINUE
}
