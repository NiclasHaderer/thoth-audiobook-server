package io.github.huiibuh.scanner

import io.ktor.util.*
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isRegularFile

val AUDIO_EXTENSIONS = setOf("mp3", "flac", "ogg", "vobis", "mp4", "m4a", "m4p", "m4b", "aiff", "wav", "wma", "dsf")

fun Path.isAudioFile(): Boolean {
    return this.isRegularFile(LinkOption.NOFOLLOW_LINKS) && this.extension.lowercase() in AUDIO_EXTENSIONS
}

fun traverseAudioFiles(directory: String, callback: (Path, BasicFileAttributes) -> Unit) {
    val file = Paths.get(directory)
    if (!Files.exists(file)) return

    Files.walkFileTree(file, AudioFileVisitor(callback))
}

fun fileExists(path: String): Boolean {
    val file = Paths.get(path)
    return Files.exists(file)
}
