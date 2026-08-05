package io.thoth.models

import org.jetbrains.exposed.v1.core.SortOrder
import java.util.UUID

data class Position(
    val sortIndex: Long,
    val id: UUID,
    val order: Order,
) {
    enum class Order {
        ASC,
        DESC,
        ;

        fun toSortOrder(): SortOrder =
            when (this) {
                ASC -> SortOrder.ASC
                DESC -> SortOrder.DESC
            }
    }
}
