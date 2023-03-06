package io.thoth.server.api.audiobooks.rescan

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S

@Resource("{id}")
data class LibraryId(
    val id: UUID_S,
)
