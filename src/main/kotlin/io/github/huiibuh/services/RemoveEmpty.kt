package io.github.huiibuh.services

import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import org.jetbrains.exposed.sql.transactions.transaction

object RemoveEmpty {
    fun series() {
        // TODO remove series with only one book in it
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
