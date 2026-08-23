package io.thoth.server.database.migrations.history

import io.thoth.server.database.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class `02_DEFER_DELETION` : Migration() {
    override fun migrate() {
        transaction {
            addColumnIfMissing("Authors")
            addColumnIfMissing("Series")
        }
    }

    private fun JdbcTransaction.addColumnIfMissing(table: String) {
        val exists =
            exec("SELECT COUNT(*) FROM pragma_table_info('$table') WHERE name = 'deferDeletionUntil'") {
                it.next()
                it.getInt(1) > 0
            } ?: false
        if (!exists) exec("ALTER TABLE \"$table\" ADD COLUMN deferDeletionUntil BIGINT NULL")
    }
}
