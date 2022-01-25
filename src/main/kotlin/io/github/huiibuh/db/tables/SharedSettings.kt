package io.github.huiibuh.db.tables

import io.github.huiibuh.db.ToModel
import io.github.huiibuh.models.KeyValueSettingsModel
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

object TKeyValueSettings : UUIDTable("KeyValueSettings") {
    val scanIndex = long("scanIndex").default(0)
}


class KeyValueSettings private constructor(
    private val id: UUID,
    private var _scanIndex: Long,
) : ToModel<KeyValueSettingsModel> {
    companion object {
        private val preferences by lazy {
            transaction {
                var dbObject = TKeyValueSettings.selectAll().firstOrNull()
                if (dbObject == null) {
                    TKeyValueSettings.insert { it[scanIndex] = 0 }
                    dbObject = TKeyValueSettings.selectAll().firstOrNull()!!
                }
                KeyValueSettings(
                    _scanIndex = dbObject!![TKeyValueSettings.scanIndex],
                    id = dbObject!![TKeyValueSettings.id].value
                )
            }
        }

        fun get(): KeyValueSettings {
            return preferences
        }

    }

    val scanIndex
        get() = this._scanIndex

    fun incrementScanIndex() {
        this._scanIndex += 1
        this.save()
    }


    private fun save() = transaction {
        TKeyValueSettings.update({ TKeyValueSettings.id eq this@KeyValueSettings.id }) {
            it[scanIndex] = this@KeyValueSettings.scanIndex
        }
    }

    override fun toModel() = KeyValueSettingsModel(scanIndex = scanIndex)
}

