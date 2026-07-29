package io.thoth.server.common.extensions

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

private fun Path.hasParent() = this.parent != null && this.parent.name.isNotEmpty()

/** This path relative to [base], or null if it is not inside [base]. */
fun Path.relativeToBase(base: Path): Path? {
    val absBase = base.toAbsolutePath().normalize()
    val absPath = this.toAbsolutePath().normalize()
    return if (absPath.startsWith(absBase)) absBase.relativize(absPath) else null
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

private val AUDIO_EXTENSIONS =
    setOf("mp3", "flac", "ogg", "opus", "aac", "m4a", "m4p", "m4b", "aiff", "wav", "wma", "dsf")

fun Path.hasAudioExtension(): Boolean = this.extension.lowercase() in AUDIO_EXTENSIONS
