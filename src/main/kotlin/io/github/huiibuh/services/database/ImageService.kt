package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Image
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object ImageService {
    fun get(id: UUID) = transaction {
        Image.findById(id)?.toModel() ?: throw APINotFound("Image could not be found")
    }
}
