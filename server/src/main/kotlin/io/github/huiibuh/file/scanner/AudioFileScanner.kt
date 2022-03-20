package io.github.huiibuh.file.scanner

import io.github.huiibuh.extensions.classLogger
import io.github.huiibuh.settings.Settings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString


class AudioFileScanner(
    private val removeSubtree: (Path) -> Unit,
    private val shouldUpdateFile: (Path) -> Boolean,
    private val addOrUpdate: (file: Path, attrs: BasicFileAttributes) -> Unit,
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
            addOrUpdate(file.absolute().normalize(), attrs)
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path, exc: IOException?) = FileVisitResult.CONTINUE

    override fun postVisitDirectory(dir: Path, exc: IOException?) = FileVisitResult.CONTINUE
}
