package io.thoth.models

class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val offset: Long,
    val limit: Int,
)
