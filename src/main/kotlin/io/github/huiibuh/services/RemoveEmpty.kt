package io.github.huiibuh.services

import io.github.huiibuh.db.tables.*
import io.github.huiibuh.db.tables.Series
import org.jetbrains.exposed.sql.transactions.transaction

object RemoveEmpty {
    fun series() {
        transaction {
            Series.all().forEach {
                if (Track.find { TTracks.series eq it.id.value }.empty()) {
                    it.delete()
                }
            }
        }
    }

    fun authors() {
        transaction {
            Author.all().forEach {
                if (Track.find { TTracks.author eq it.id.value }.empty()) {
                    it.delete()
                }
            }
        }
    }

    fun books() {
        transaction {
            Book.all().forEach {
                if (Track.find { TTracks.book eq it.id.value }.empty()) {
                    it.delete()
                }
            }
        }
    }
}
