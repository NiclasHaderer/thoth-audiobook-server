package io.thoth.server.common

import io.ktor.http.ContentType
import java.nio.file.Path
import kotlin.io.path.extension

val AUDIO_TYPES: Map<String, ContentType> =
    mapOf(
        "mp3" to ContentType("audio", "mpeg"),
        "flac" to ContentType("audio", "flac"),
        "ogg" to ContentType("audio", "ogg"),
        "opus" to ContentType("audio", "ogg"),
        "aac" to ContentType("audio", "aac"),
        "m4a" to ContentType("audio", "mp4"),
        "m4b" to ContentType("audio", "mp4"),
        "m4p" to ContentType("audio", "mp4"),
        "aiff" to ContentType("audio", "aiff"),
        "wav" to ContentType("audio", "wav"),
        "wma" to ContentType("audio", "x-ms-wma"),
        "dsf" to ContentType("audio", "x-dsf"),
    )

fun audioContentType(path: Path): ContentType? = AUDIO_TYPES[path.extension.lowercase()]
