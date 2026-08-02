package io.thoth.server.file.scanner

import io.thoth.server.common.extensions.hasAudioExtension
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute
import kotlin.io.path.exists

private class FileTreeScanner(
    private val ignoreFolder: (Path) -> Unit,
    private val addOrUpdate: (file: Path, attrs: BasicFileAttributes) -> Unit,
) : FileVisitor<Path> {
    private val log = logger {}

    override fun preVisitDirectory(
        dir: Path,
        attrs: BasicFileAttributes,
    ): FileVisitResult =
        if (!hasIgnoreMarker(dir)) {
            FileVisitResult.CONTINUE
        } else {
            ignoreFolder(dir.absolute().normalize())
            log.debug { "Ignoring directory ${dir.absolute().normalize()}" }
            FileVisitResult.SKIP_SUBTREE
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

// Both sides of a "is this path inside that root" test have to be canonicalised the same way, or a
// symlinked ancestor (/var on macOS) makes them compare unequal. Deleted paths are resolved through their
// nearest surviving ancestor, since the watcher reports them after they are gone.
fun realPath(path: Path): Path {
    val absolute = path.absolute().normalize()
    val existing = generateSequence(absolute) { it.parent }.firstOrNull { it.exists() } ?: return absolute
    return runCatching { existing.toRealPath().resolve(existing.relativize(absolute)) }.getOrDefault(absolute)
}

fun libraryRoot(folder: String): Path = realPath(Path.of(folder))

fun walkFiles(
    rootDirectory: Path,
    ignoreFolder: (Path) -> Unit,
    addOrUpdate: (file: Path, attrs: BasicFileAttributes) -> Unit,
) {
    // Links inside the tree are not followed: the watcher does not follow them either, so a symlinked
    // folder would be scanned but never watched, and a link to a folder already in the library imports
    // every track a second time under a second path.
    Files.walkFileTree(rootDirectory, FileTreeScanner(ignoreFolder, addOrUpdate))
}
