package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.models.Album
import io.github.huiibuh.db.models.Albums
import io.github.huiibuh.db.models.Collection
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object CollectionService {

    fun getCollections(limit: Int, offset: Long) = transaction {
        Collection.all().limit(limit, offset).map {
            it.toModel()
        }
    }

    fun getBooks(id: UUID) = transaction {
        Album.find { Albums.collection eq id }.map { it.toModel() }
    }

    fun get(id: UUID) = transaction {
        Collection.findById(id)?.toModel() ?: throw APINotFound("Could not find collection")
    }
}
