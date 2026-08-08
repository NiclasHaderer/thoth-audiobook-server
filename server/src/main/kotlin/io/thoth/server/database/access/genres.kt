package io.thoth.server.database.access

import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.database.tables.GenresTable
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

context(_: Transaction)
fun getOrCreateGenres(names: List<String>): List<UUID> =
    names.distinctBy { it.lowercase() }.map { name ->
        GenresTable
            .select(GenresTable.id)
            .where { GenresTable.name ilike escape(name) }
            .firstOrNull()
            ?.get(GenresTable.id)
            ?.value
            ?: GenresTable.insertAndGetId { it[GenresTable.name] = name }.value
    }
