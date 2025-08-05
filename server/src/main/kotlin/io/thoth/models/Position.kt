package io.thoth.models

import java.util.*
import org.jetbrains.exposed.sql.SortOrder

data class Position(val sortIndex: Long, val id: UUID, val order: Order) {
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
