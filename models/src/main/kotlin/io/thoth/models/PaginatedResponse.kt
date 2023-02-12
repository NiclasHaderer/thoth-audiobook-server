package io.thoth.models

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val offset: Long,
    val limit: Int,
)
