package io.github.huiibuh.services.database

import io.github.huiibuh.db.tables.TKeyValueSettings
import io.github.huiibuh.models.KeyValueSettings
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object KeyValueSettingsService {
    fun get() = transaction {
        val preferences = queryFor()

        KeyValueSettings(
            scanIndex = preferences[TKeyValueSettings.scanIndex]
        )
    }

    fun save(settings: KeyValueSettings) = transaction {
        val preferences = queryFor()
        TKeyValueSettings.update({ TKeyValueSettings.id eq preferences[TKeyValueSettings.id] }) {
            it[scanIndex] = settings.scanIndex
        }
    }

    private fun queryFor() = transaction {
        var preferences = TKeyValueSettings.selectAll().firstOrNull()
        if (preferences == null) {
            TKeyValueSettings.insert { it[scanIndex] = 0 }
            preferences = TKeyValueSettings.selectAll().firstOrNull()!!
        }
        preferences
    }
}
