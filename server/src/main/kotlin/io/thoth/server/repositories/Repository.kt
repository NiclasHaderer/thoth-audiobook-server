package io.thoth.server.repositories

import org.jetbrains.exposed.v1.core.SortOrder
import java.util.UUID

// Grace period during which a manually created (or freshly edited) author/series survives
// orphan cleanup even though no book links to it yet
const val DEFER_DELETION_GRACE_MS = 60 * 60 * 1000L

interface Repository<RAW, NORMAL, DETAILED, PARTIAL_API> {
    val searchLimit: Int
        get() = 30

    fun raw(
        id: UUID,
        libraryId: UUID,
    ): RAW

    fun get(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): DETAILED

    fun getAll(
        userId: UUID,
        libraryId: UUID,
        order: SortOrder,
        limit: Int = 20,
        offset: Long = 0L,
    ): List<NORMAL>

    fun search(
        userId: UUID,
        query: String,
        libraryId: UUID,
    ): List<NORMAL>

    fun search(
        userId: UUID,
        query: String,
    ): List<NORMAL>

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
        userId: UUID,
        id: UUID,
        libraryId: UUID,
        partial: PARTIAL_API,
    ): NORMAL

    fun autoMatch(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): NORMAL

    fun total(libraryId: UUID): Long
}
