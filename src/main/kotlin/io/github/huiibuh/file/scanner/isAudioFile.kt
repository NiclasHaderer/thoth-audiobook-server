package io.github.huiibuh.file.scanner

import io.ktor.util.*
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.isRegularFile

val AUDIO_EXTENSIONS = setOf("mp3", "flac", "ogg", "vobis", "m4a", "m4p", "m4b", "aiff", "wav", "wma", "dsf")

fun Path.isAudioFile(): Boolean {
    return this.isRegularFile(LinkOption.NOFOLLOW_LINKS) && this.hasAudioExtension()
}

fun Path.hasAudioExtension(): Boolean {
    return this.extension.lowercase() in AUDIO_EXTENSIONS
}
