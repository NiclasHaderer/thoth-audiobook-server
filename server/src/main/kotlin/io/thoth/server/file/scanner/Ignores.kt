package io.thoth.server.file.scanner

import io.methvin.watcher.visitor.FileTreeVisitor
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute
import kotlin.io.path.exists

const val IGNORE_FILE = ".thothignore"

fun hasIgnoreMarker(folder: Path): Boolean = folder.resolve(IGNORE_FILE).exists()

fun isIgnored(
    path: Path,
    root: Path,
): Boolean {
    val normalizedRoot = root.absolute().normalize()
    return generateSequence(path.absolute().normalize()) { it.parent }
        .takeWhile { it.startsWith(normalizedRoot) }
        .any { hasIgnoreMarker(it) }
}

// Consulted for the watcher's start-up index, and on Linux for the watch registrations too. Where the
// FILE_TREE modifier works (macOS, Windows) the OS watches the whole tree regardless, so events from
// ignored folders still arrive and have to be filtered when they do.
object IgnoreAwareVisitor : FileTreeVisitor {
    override fun recursiveVisitFiles(
        file: Path,
        onDirectory: FileTreeVisitor.Callback,
        onFile: FileTreeVisitor.Callback,
    ) {
        Files.walkFileTree(
            file,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    if (hasIgnoreMarker(dir)) return FileVisitResult.SKIP_SUBTREE
                    onDirectory.call(dir)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    f: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    onFile.call(f)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(
                    f: Path,
                    exc: IOException,
                ) = FileVisitResult.CONTINUE
            },
        )
    }
}
