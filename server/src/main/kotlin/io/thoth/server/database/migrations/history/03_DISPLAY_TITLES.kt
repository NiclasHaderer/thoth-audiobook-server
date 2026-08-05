package io.thoth.server.database.migrations.history

import io.thoth.server.database.migrations.Migration
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.SeriesTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.vendors.currentDialectMetadata

class `03_DISPLAY_TITLES` : Migration() {
    override fun migrate() {
        transaction {
            addIfMissing(BooksTable.displayTitle)
            addIfMissing(SeriesTable.displayTitle)
        }
    }

    // Databases created after this migration landed already got the column from the initial setup
    private fun JdbcTransaction.addIfMissing(column: Column<*>) {
        val present =
            currentDialectMetadata
                .tableColumns(column.table)[column.table]
                .orEmpty()
                .any { it.name.equals(column.name, ignoreCase = true) }
        if (!present) column.createStatement().forEach { exec(it) }
    }
}
