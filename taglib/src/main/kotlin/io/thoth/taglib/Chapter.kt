package io.thoth.taglib

/** [endMs] is null for formats that only store chapter start times, such as MP4. */
data class Chapter(
    val title: String?,
    val startMs: Long,
    val endMs: Long?,
)
