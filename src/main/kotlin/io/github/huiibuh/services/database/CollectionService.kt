package io.github.huiibuh.services.database

import io.github.huiibuh.db.models.Collection
import org.jetbrains.exposed.sql.transactions.transaction

object CollectionService {

    fun getCollections(limit: Int, offset: Long) = transaction {
        Collection.all().limit(limit, offset).map {
            it.toModel()
        }
    }
}
