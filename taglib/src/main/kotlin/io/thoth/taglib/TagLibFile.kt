package io.thoth.taglib

import io.thoth.taglib.ffi.TagLibC
import io.thoth.taglib.ffi.TagLib_Chapter
import io.thoth.taglib.ffi.TagLib_Complex_Property_Picture_Data
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Read-only view of the tags of a single audio file, backed by TagLib.
 *
 * Not thread safe. Instances hold a native file handle and must be closed.
 */
class TagLibFile(path: Path) : AutoCloseable {
    private val arena = Arena.ofConfined()
    private val file: MemorySegment
    private var closed = false

    init {
        NativeLoader.ensureLoaded()
        val handle = TagLibC.taglib_file_new(arena.allocateFrom(path.toString(), StandardCharsets.UTF_8))
        // tag_c hands back a FileRef even for a path it could not open, so an unreadable
        // file or an unrecognized format only shows up as "not valid".
        if (handle.isNull() || TagLibC.taglib_file_is_valid(handle) == 0) {
            if (!handle.isNull()) TagLibC.taglib_file_free(handle)
            arena.close()
            throw TagLibException("TagLib could not read $path")
        }
        file = handle
    }

    fun isValid(): Boolean = !closed && TagLibC.taglib_file_is_valid(file) != 0

    /**
     * TagLib's normalized property keys, e.g. `TITLE`, `ARTIST`, `WORK`; values are multi-valued.
     * Keys without any value are omitted, so a present key always has at least one value. Values
     * are returned as stored, blank ones included.
     */
    fun properties(): Map<String, List<String>> {
        checkOpen()
        val keys = TagLibC.taglib_property_keys(file)
        if (keys.isNull()) return emptyMap()
        try {
            return keys.pointers().associate { key ->
                val values = TagLibC.taglib_property_get(file, key)
                val strings =
                    if (values.isNull()) {
                        emptyList()
                    } else {
                        try {
                            values.pointers().map { it.string() }
                        } finally {
                            TagLibC.taglib_property_free(values)
                        }
                    }
                key.string() to strings
            }.filterValues { it.isNotEmpty() }
        } finally {
            TagLibC.taglib_property_free(keys)
        }
    }

    val lengthInSeconds: Int
        get() = audioProperties { TagLibC.taglib_audioproperties_length(it) } ?: 0

    val bitrate: Int
        get() = audioProperties { TagLibC.taglib_audioproperties_bitrate(it) } ?: 0

    val sampleRate: Int
        get() = audioProperties { TagLibC.taglib_audioproperties_samplerate(it) } ?: 0

    val channels: Int
        get() = audioProperties { TagLibC.taglib_audioproperties_channels(it) } ?: 0

    /**
     * Entries without image bytes are dropped, so [Picture.data] is never empty: the mime type and
     * description of an image that is not there are of no use to a caller.
     */
    fun pictures(): List<Picture> {
        checkOpen()
        val pictures = TagLibC.taglib_file_pictures(file)
        if (pictures.isNull()) return emptyList()
        try {
            return pictures.pointers().mapNotNull { pointer ->
                val picture = pointer.reinterpret(TagLib_Complex_Property_Picture_Data.sizeof())
                val size = TagLib_Complex_Property_Picture_Data.size(picture)
                val data = TagLib_Complex_Property_Picture_Data.data(picture)
                if (data.isNull() || size <= 0) return@mapNotNull null
                Picture(
                    data = data.reinterpret(size.toLong()).toArray(ValueLayout.JAVA_BYTE),
                    mimeType = TagLib_Complex_Property_Picture_Data.mimeType(picture).stringOrNull(),
                    description =
                        TagLib_Complex_Property_Picture_Data.description(picture).stringOrNull(),
                    type = TagLib_Complex_Property_Picture_Data.pictureType(picture).stringOrNull(),
                )
            }
        } finally {
            TagLibC.taglib_pictures_free(pictures)
        }
    }

    /**
     * Chapters in file order. MP4 (Nero/QuickTime) and ID3v2 CHAP frames come from TagLib itself;
     * Ogg and FLAC store chapters as ordinary Vorbis comments, which are parsed from [properties].
     */
    fun chapters(): List<Chapter> {
        checkOpen()
        val chapters = TagLibC.taglib_file_chapters(file)
        if (chapters.isNull()) return vorbisCommentChapters()
        try {
            return chapters.pointers().map { pointer ->
                val chapter = pointer.reinterpret(TagLib_Chapter.sizeof())
                Chapter(
                    title = TagLib_Chapter.title(chapter).stringOrNull(),
                    startMs = TagLib_Chapter.startTime(chapter),
                    endMs = TagLib_Chapter.endTime(chapter).takeIf { it >= 0 },
                )
            }
        } finally {
            TagLibC.taglib_chapters_free(chapters)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        TagLibC.taglib_file_free(file)
        arena.close()
    }

    /**
     * The `CHAPTER001=00:00:00.000` / `CHAPTER001NAME=...` convention used by Ogg and FLAC. TagLib
     * surfaces these as plain properties, so there is nothing format specific to call.
     */
    private fun vorbisCommentChapters(): List<Chapter> {
        val properties = properties()
        return properties.keys
            .mapNotNull { CHAPTER_KEY.matchEntire(it) }
            .mapNotNull { match ->
                val number = match.groupValues[1]
                val timestamp = properties["CHAPTER$number"]?.firstOrNull() ?: return@mapNotNull null
                val startMs = parseTimestamp(timestamp) ?: return@mapNotNull null
                number.toInt() to Chapter(
                    title = properties["CHAPTER${number}NAME"]?.firstOrNull(),
                    startMs = startMs,
                    endMs = null,
                )
            }
            .sortedBy { it.first }
            .map { it.second }
    }

    private inline fun audioProperties(read: (MemorySegment) -> Int): Int? {
        checkOpen()
        val properties = TagLibC.taglib_file_audioproperties(file)
        return if (properties.isNull()) null else read(properties)
    }

    private fun checkOpen() {
        if (closed) throw IllegalStateException("TagLibFile is closed")
    }

    private companion object {
        /** The Xiph chapter extension numbers chapters `000` to `999`. */
        val CHAPTER_KEY = Regex("""CHAPTER(\d{1,3})""")

        /**
         * `hh:mm:ss.sss` per the Xiph chapter extension. The fraction is read at any precision and
         * truncated: mkvmerge documents `HH:MM:SS.nnnnnnnnn` and ffmpeg accepts it, so rejecting a
         * nanosecond timestamp would silently drop the chapter.
         */
        fun parseTimestamp(timestamp: String): Long? {
            val match = Regex("""(\d{1,3}):(\d{1,2}):(\d{1,2})(?:\.(\d+))?""").matchEntire(timestamp.trim())
                ?: return null
            val (hours, minutes, seconds, fraction) = match.destructured
            return hours.toLong() * 3_600_000 +
                minutes.toLong() * 60_000 +
                seconds.toLong() * 1000 +
                fraction.take(3).padEnd(3, '0').toLong()
        }

        fun MemorySegment.isNull() = equals(MemorySegment.NULL)

        /** Reads a NULL terminated array of pointers, which is how tag_c returns collections. */
        fun MemorySegment.pointers(): List<MemorySegment> {
            val array = reinterpret(Long.MAX_VALUE)
            val result = mutableListOf<MemorySegment>()
            var offset = 0L
            while (true) {
                val pointer = array.get(ValueLayout.ADDRESS, offset)
                if (pointer.isNull()) return result
                result += pointer
                offset += ValueLayout.ADDRESS.byteSize()
            }
        }

        fun MemorySegment.string(): String =
            reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_8)

        fun MemorySegment.stringOrNull(): String? = if (isNull()) null else string().ifEmpty { null }
    }
}

class TagLibException(message: String) : RuntimeException(message)
