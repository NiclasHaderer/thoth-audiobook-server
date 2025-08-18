package io.thoth.server.repositories

import org.jetbrains.exposed.v1.core.SortOrder
import java.util.UUID

interface Repository<RAW, NORMAL, DETAILED, PARTIAL_API, COMPLETE_API> {
    val searchLimit: Int
        get() = 30

    fun raw(
        id: UUID,
        libraryId: UUID,
    ): RAW

    fun get(
        id: UUID,
        libraryId: UUID,
    ): DETAILED

    fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int = 20,
        offset: Long = 0L,
    ): List<NORMAL>

    fun search(
        query: String,
        libraryId: UUID,
    ): List<NORMAL>

    fun search(query: String): List<NORMAL>

    fun sorting(
        libraryId: UUID,
        order: SortOrder,
        limit: Int = 20,
        offset: Long = 0L,
    ): List<UUID>

    fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
    ): Long

    fun modify(
        id: UUID,
        libraryId: UUID,
        partial: PARTIAL_API,
    ): NORMAL

    fun replace(
        id: UUID,
        libraryId: UUID,
        complete: COMPLETE_API,
    ): NORMAL

    fun autoMatch(
        id: UUID,
        libraryId: UUID,
    ): NORMAL

    fun total(libraryId: UUID): Long
}
