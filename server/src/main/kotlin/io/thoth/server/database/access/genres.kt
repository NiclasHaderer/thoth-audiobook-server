package io.thoth.server.database.access

import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.database.tables.GenreEntity
import io.thoth.server.database.tables.GenresTable

/** Tags spell genres inconsistently ("Sci-Fi" vs "sci-fi"), so an existing row is reused case insensitively. */
fun GenreEntity.Companion.getOrCreate(names: List<String>): List<GenreEntity> =
    names.distinctBy { it.lowercase() }.map { name ->
        GenreEntity.find { GenresTable.name ilike escape(name) }.firstOrNull()
            ?: GenreEntity.new { this.name = name }
    }
