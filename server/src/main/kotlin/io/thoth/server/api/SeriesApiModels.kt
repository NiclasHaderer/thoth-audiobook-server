package io.thoth.server.api

import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable

@Serializable
data class PatchSeries(
    val title: String?,
    val authors: List<UUID_S>?,
    val books: List<UUID_S>?,
    val provider: String?,
    val providerID: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val cover: String?,
    val description: String?
) {
    init {
        require(authors?.isNotEmpty() ?: true) { "Authors must not be empty" }
        require(books?.isNotEmpty() ?: true) { "Books must not be empty" }
    }
}

@Serializable
data class PutSeries(
    val title: String,
    val authors: List<UUID_S>,
    val books: List<UUID_S>,
    val provider: String?,
    val providerID: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val cover: String?,
    val description: String?,
) {
    init {
        require(authors.isNotEmpty()) { "Authors must not be empty" }
        require(books.isNotEmpty()) { "Books must not be empty" }
    }
}
