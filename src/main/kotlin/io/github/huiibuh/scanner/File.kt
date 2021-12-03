package io.github.huiibuh.scanner

import io.github.huiibuh.db.tables.Track
import io.ktor.util.*
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isRegularFile

val AUDIO_EXTENSIONS = setOf("mp3", "flac", "ogg", "vobis", "m4a", "m4p", "m4b", "aiff", "wav", "wma", "dsf")

fun Path.isAudioFile(): Boolean {
    return this.isRegularFile(LinkOption.NOFOLLOW_LINKS) && this.extension.lowercase() in AUDIO_EXTENSIONS
}

fun traverseAudioFiles(
    directory: String,
    add: suspend (TrackReference, BasicFileAttributes, Path, Track?) -> Unit,
    removeSubtree: (Path) -> Unit,
) {
    val file = Paths.get(directory)
    if (!Files.exists(file)) return

    Files.walkFileTree(file, AudioFileVisitor(add = add, removeSubtree = removeSubtree))
}
