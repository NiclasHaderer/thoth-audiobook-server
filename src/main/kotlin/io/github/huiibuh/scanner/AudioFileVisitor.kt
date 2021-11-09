package io.github.huiibuh.scanner

import io.github.huiibuh.config.Settings
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolutePathString

class AudioFileVisitor(private val callback: (TrackReference, BasicFileAttributes, Path) -> Unit) : FileVisitor<Path> {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
        val ignoreFile =
            Paths.get("${dir.absolutePathString()}${FileSystems.getDefault().separator}${Settings.ignoreFile}")
        return if (!Files.exists(ignoreFile)) {
            FileVisitResult.CONTINUE
        } else {
            FileVisitResult.SKIP_SUBTREE
        }
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (file.isAudioFile()) {
            val audioFile = TrackReference.fromPath(file.absolutePathString())
            if (audioFile.hasRequiredAttributes()) {
                callback(audioFile, attrs, file)
            } else {
                log.info("File ${file.absolutePathString()} is missing artist and album tags. ")
                log.info("The file will be ignored")
            }
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

}
