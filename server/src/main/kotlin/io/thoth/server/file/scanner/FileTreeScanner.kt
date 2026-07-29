package io.thoth.server.file.scanner

import io.thoth.server.common.extensions.hasAudioExtension
import io.thoth.server.config.ThothConfig
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

private class FileTreeScanner(
    private val ignoreFolder: (Path) -> Unit,
    private val addOrUpdate: (file: Path, attrs: BasicFileAttributes) -> Unit,
) : FileVisitor<Path>,
    KoinComponent {
    private val thothConfig: ThothConfig by inject()
    private val log = logger {}

    override fun preVisitDirectory(
        dir: Path,
        attrs: BasicFileAttributes,
    ): FileVisitResult {
        val ignoreFile = Paths.get(dir.absolutePathString(), thothConfig.ignoreFile)
        return if (!Files.exists(ignoreFile)) {
            FileVisitResult.CONTINUE
        } else {
            ignoreFolder(dir.absolute().normalize())
            log.debug { "Ignoring directory ${dir.absolute().normalize()}" }
            FileVisitResult.SKIP_SUBTREE
        }
    }

    override fun visitFile(
        file: Path,
        attrs: BasicFileAttributes,
    ): FileVisitResult {
        if (attrs.isRegularFile && file.hasAudioExtension()) {
            addOrUpdate(file.absolute().normalize(), attrs)
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(
        file: Path,
        exc: IOException,
    ) = FileVisitResult.CONTINUE

    override fun postVisitDirectory(
        dir: Path,
        exc: IOException?,
    ) = FileVisitResult.CONTINUE
}

fun walkFiles(
    rootDirectory: Path,
    ignoreFolder: (Path) -> Unit,
    addOrUpdate: (file: Path, attrs: BasicFileAttributes) -> Unit,
) {
    val fileTreeScanner = FileTreeScanner(ignoreFolder, addOrUpdate)
    // Follow links so symlinked libraries and books get scanned. Link loops surface as visitFileFailed, which skips.
    Files.walkFileTree(rootDirectory, setOf(FileVisitOption.FOLLOW_LINKS), Int.MAX_VALUE, fileTreeScanner)
}
