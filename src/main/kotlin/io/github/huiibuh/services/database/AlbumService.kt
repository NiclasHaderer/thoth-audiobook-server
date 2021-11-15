package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Album
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object AlbumService {
    fun getMultiple(limit: Int, offset: Long) = transaction {
        Album.all().limit(limit, offset).map { it.toModel() }
    }

    fun get(uuid: UUID) = transaction {
        Album.findById(uuid)?.toModel() ?: throw APINotFound("Could not find album")
    }
}
