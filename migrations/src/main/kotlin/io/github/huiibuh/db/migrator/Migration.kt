package io.github.huiibuh.db.migrator

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

abstract class Migration {
    abstract fun migrate(db: Database)
    abstract fun rollback(db: Database)


    fun addMissingColumns(vararg tables: Table) {
        transaction {
            val statements = SchemaUtils.addMissingColumnsStatements(*tables)
            if (statements.isNotEmpty())
                execInBatch(statements)
        }
    }
}
