package io.thoth.models

import java.util.UUID

enum class ChangedEntity {
    LIBRARY,
    BOOK,
    AUTHOR,
    SERIES,
}

enum class ChangeType {
    CREATED,
    UPDATED,
    DELETED,
}

data class ChangeEvent(
    val entity: ChangedEntity,
    val type: ChangeType,
    val id: UUID,
    val libraryId: UUID,
)

data class ChangeEventBatch(
    val events: List<ChangeEvent>,
)
