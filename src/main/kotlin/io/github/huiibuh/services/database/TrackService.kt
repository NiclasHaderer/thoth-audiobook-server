package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.models.Track
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object TrackService {
    fun get(uuid: UUID): Track = transaction {
        Track.findById(uuid) ?: throw APINotFound("Requested track was not found")
    }
}
