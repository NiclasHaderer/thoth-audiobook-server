package io.github.huiibuh.services.database

import io.github.huiibuh.db.tables.TSharedSettings
import io.github.huiibuh.models.SharedSettings
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object SharedSettingsService {
    fun get() = transaction {
        val preferences = queryFor()

        SharedSettings(
            scanIndex = preferences[TSharedSettings.scanIndex]
        )
    }

    fun save(settings: SharedSettings) = transaction {
        val preferences = queryFor()
        TSharedSettings.update({ TSharedSettings.id eq preferences[TSharedSettings.id] }) {
            it[scanIndex] = settings.scanIndex
        }
    }

    private fun queryFor() = transaction {
        var preferences = TSharedSettings.selectAll().firstOrNull()
        if (preferences == null) {
            TSharedSettings.insert { it[scanIndex] = 0 }
            preferences = TSharedSettings.selectAll().firstOrNull()!!
        }
        preferences
    }
}
