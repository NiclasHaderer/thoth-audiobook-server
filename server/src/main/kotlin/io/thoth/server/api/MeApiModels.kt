package io.thoth.server.api

data class ProgressUpdate(
    val positionMs: Long,
)

data class SetFinished(
    val finished: Boolean,
)

data class SetDismissed(
    val dismissed: Boolean,
)
