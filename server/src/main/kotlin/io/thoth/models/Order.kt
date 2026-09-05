package io.thoth.models

import org.jetbrains.exposed.v1.core.SortOrder

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
