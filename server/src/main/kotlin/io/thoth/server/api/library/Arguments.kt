package io.thoth.server.api.library

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S

@Resource("{id}")
data class LibraryId(
    val id: UUID_S,
)

data class PostLibrary(
    val name: String,
    val icon: String?,
    val folders: List<String>,
    val preferEmbeddedMetadata: Boolean,
) {
    init {
        require(folders.isNotEmpty())
    }
}

data class PatchLibrary(
    val name: String?,
    val icon: String?,
    val folders: List<String>?,
    val preferEmbeddedMetadata: Boolean?,
) {
    init {
        require(folders == null || folders.isNotEmpty())
    }
}
