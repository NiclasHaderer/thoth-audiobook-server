package io.thoth.models

import org.jetbrains.exposed.sql.SortOrder
import java.util.*

data class Position(
    val sortIndex: Long,
    val id: UUID,
    val order: Order,
) {
    enum class Order(
        val order: String,
    ) {
        ASC("ASC"),
        DESC("DESC"),
        ;

        fun toSortOrder(): SortOrder =
            when (this) {
                ASC -> SortOrder.ASC
                DESC -> SortOrder.DESC
            }
    }
}
