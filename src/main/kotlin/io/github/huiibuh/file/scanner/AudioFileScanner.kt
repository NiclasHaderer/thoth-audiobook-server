package io.github.huiibuh.file.scanner

import io.github.huiibuh.file.analyzer.AudioFileAnalysisValue
import io.github.huiibuh.file.analyzer.AudioFileAnalyzer
import io.github.huiibuh.settings.Settings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolutePathString


class AudioFileScanner(
    private val pathHandler: AudioFileAnalyzer,
    private val removeSubtree: (Path) -> Unit,
    private val shouldUpdateFile: (Path) -> Boolean,
    private val addOrUpdate: (file: Path, attrs: BasicFileAttributes, result: AudioFileAnalysisValue) -> Unit,
) : FileVisitor<Path>, KoinComponent {
    private val settings: Settings by inject()
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
        val ignoreFile =
            Paths.get("${dir.absolutePathString()}${FileSystems.getDefault().separator}${settings.ignoreFile}")
        return if (!Files.exists(ignoreFile)) {
            FileVisitResult.CONTINUE
        } else {
            removeSubtree(dir)
            FileVisitResult.SKIP_SUBTREE
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (file.isAudioFile() && shouldUpdateFile(file)) {
            val (success, value) = runBlocking { pathHandler.analyze(file, attrs) }
            if (success) {
                if (value == null) {
                    log.warn("One of the file analyzers returned success with a null value. This is not allowed")
                    return FileVisitResult.CONTINUE
                }
                addOrUpdate(file, attrs, value)
            }
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path, exc: IOException?) = FileVisitResult.CONTINUE

    override fun postVisitDirectory(dir: Path, exc: IOException?) = FileVisitResult.CONTINUE
}
