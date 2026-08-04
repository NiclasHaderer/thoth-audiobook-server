package io.thoth.server.file.scanner

import io.methvin.watcher.visitor.FileTreeVisitor
import io.thoth.server.common.extensions.canonical
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.exists

const val IGNORE_FILE = ".thothignore"

fun hasIgnoreMarker(folder: Path): Boolean = folder.resolve(IGNORE_FILE).exists()

fun isIgnored(
    path: Path,
    root: Path,
): Boolean {
    val normalizedRoot = root.canonical()
    return generateSequence(path.canonical()) { it.parent }
        .takeWhile { it.startsWith(normalizedRoot) }
        .any { hasIgnoreMarker(it) }
}

// We do not follow symlinks...
fun walkIgnoreAware(
    root: Path,
    onDirectory: (Path) -> Unit = {},
    onIgnoredDirectory: (Path) -> Unit = {},
    onFailure: (path: Path, failure: IOException) -> Unit = { _, _ -> },
    onFile: (file: Path, attrs: BasicFileAttributes) -> Unit = { _, _ -> },
) {
    Files.walkFileTree(
        root,
        object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(
                dir: Path,
                attrs: BasicFileAttributes,
            ): FileVisitResult {
                if (hasIgnoreMarker(dir)) {
                    onIgnoredDirectory(dir)
                    return FileVisitResult.SKIP_SUBTREE
                }
                onDirectory(dir)
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(
                f: Path,
                attrs: BasicFileAttributes,
            ): FileVisitResult {
                onFile(f, attrs)
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(
                f: Path,
                exc: IOException,
            ): FileVisitResult {
                onFailure(f, exc)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(
                dir: Path,
                exc: IOException?,
            ): FileVisitResult {
                if (exc != null) onFailure(dir, exc)
                return FileVisitResult.CONTINUE
            }
        },
    )
}

object IgnoreAwareVisitor : FileTreeVisitor {
    override fun recursiveVisitFiles(
        file: Path,
        onDirectory: FileTreeVisitor.Callback,
        onFile: FileTreeVisitor.Callback,
    ) = walkIgnoreAware(
        file,
        onDirectory = { onDirectory.call(it) },
        onFile = { f, _ -> onFile.call(f) },
    )
}
