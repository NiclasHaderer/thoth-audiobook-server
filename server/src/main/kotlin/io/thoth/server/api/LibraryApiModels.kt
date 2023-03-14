package io.thoth.server.api

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
