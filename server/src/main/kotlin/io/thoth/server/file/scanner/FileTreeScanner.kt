package io.thoth.server.file.scanner

import io.thoth.common.extensions.isAudioFile
import io.thoth.config.ThothConfig
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
import mu.KotlinLogging.logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private class FileTreeScanner(
    private val ignoredSubtree: (Path) -> Unit,
    private val shouldUpdateFile: (Path) -> Boolean,
    private val addOrUpdate: (file: Path, attrs: BasicFileAttributes) -> Unit,
) : FileVisitor<Path>, KoinComponent {
    private val thothConfig: ThothConfig by inject()
    private val log = logger {}

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
        val ignoreFile =
            Paths.get(
                "${dir.absolutePathString()}${FileSystems.getDefault().separator}${thothConfig.ignoreFile}",
            )
        return if (!Files.exists(ignoreFile)) {
            FileVisitResult.CONTINUE
        } else {
            ignoredSubtree(dir.absolute().normalize())
            log.debug { "Ignoring directory ${dir.absolute().normalize()}" }
            FileVisitResult.SKIP_SUBTREE
        }
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (file.isAudioFile() && shouldUpdateFile(file.normalize())) {
            addOrUpdate(file.absolute().normalize(), attrs)
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path, exc: IOException?) = FileVisitResult.CONTINUE

    override fun postVisitDirectory(dir: Path, exc: IOException?) = FileVisitResult.CONTINUE
}

fun walkFiles(
    startingDirectory: Path,
    ignoredSubtree: (Path) -> Unit,
    shouldUpdateFile: (Path) -> Boolean,
    addOrUpdate: (file: Path, attrs: BasicFileAttributes) -> Unit,
) {
    val fileTreeScanner = FileTreeScanner(ignoredSubtree, shouldUpdateFile, addOrUpdate)
    Files.walkFileTree(startingDirectory, fileTreeScanner)
}
