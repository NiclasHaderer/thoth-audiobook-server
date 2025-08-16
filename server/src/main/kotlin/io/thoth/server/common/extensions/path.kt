package io.thoth.server.common.extensions

import io.ktor.util.extension
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

fun Path.hasParent() = this.parent != null && this.parent.name.isNotEmpty()

fun Path.replacePart(replaceString: String): Path {
    val absReplace =
        Path
            .of(replaceString)
            .toAbsolutePath()
            .normalize()
            .absolutePathString()
    val absPath = this.toAbsolutePath().normalize().absolutePathString()
    return Path.of(absPath.replace(absReplace, ""))
}

fun Path.countParents(): Int {
    var current = this.normalize()
    var parents = 0
    while (current.hasParent()) {
        current = current.parent
        parents += 1
    }
    return parents
}

fun Path.parentName() = this.parent.name

fun Path.grandParentName() = this.parent.parent.name

fun Path.grandGrandParentName() = this.parent.parent.parent.name

fun Path.replaceParts(parts: List<String>): Path {
    var result = this
    parts.forEach { result = result.replacePart(it) }
    return result
}

private val AUDIO_EXTENSIONS = setOf("mp3", "flac", "ogg", "vobis", "m4a", "m4p", "m4b", "aiff", "wav", "wma", "dsf")

fun Path.isAudioFile(): Boolean = this.isRegularFile(LinkOption.NOFOLLOW_LINKS) && this.hasAudioExtension()

fun Path.hasAudioExtension(): Boolean = this.extension.lowercase() in AUDIO_EXTENSIONS
