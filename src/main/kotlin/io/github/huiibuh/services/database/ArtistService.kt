package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Artist
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object ArtistService {
    fun get(uuid: UUID) = transaction {
        Artist.findById(uuid)?.toModel() ?: throw APINotFound("Could not find artist")
    }

    fun getMultiple(limit: Int, offset: Long) = transaction {
        Artist.all().limit(limit, offset).map { it.toModel() }
    }
}
