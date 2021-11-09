package io.github.huiibuh.services

import io.github.huiibuh.db.models.*
import io.github.huiibuh.db.models.Collection
import org.jetbrains.exposed.sql.transactions.transaction

object RemoveEmpty {
    fun collections() {
        transaction {
            Collection.all().forEach {
                if (Track.find { Tracks.collection eq it.id.value }.empty()) {
                    it.delete()
                }
            }
        }
    }

    fun artists() {
        transaction {
            Artist.all().forEach {
                if (Track.find { Tracks.artist eq it.id.value }.empty()) {
                    it.delete()
                }
            }
        }
    }

    fun albums() {
        transaction {
            Album.all().forEach {
                if (Track.find { Tracks.album eq it.id.value }.empty()) {
                    it.delete()
                }
            }
        }
    }
}
