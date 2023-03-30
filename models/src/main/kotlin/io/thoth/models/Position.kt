package io.thoth.models

import io.thoth.common.serializion.kotlin.UUID_S
import org.jetbrains.exposed.sql.SortOrder

data class Position(val sortIndex: Long, val id: UUID_S, val order: Order) {
    enum class Order(val order: String) {
        ASC("ASC"),
        DESC("DESC");

        fun toSortOrder(): SortOrder {
            return when (this) {
                ASC -> SortOrder.ASC
                DESC -> SortOrder.DESC
            }
        }
    }
}
