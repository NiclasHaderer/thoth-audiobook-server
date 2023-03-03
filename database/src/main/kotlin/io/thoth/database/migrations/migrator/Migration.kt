package io.thoth.database.migrations.migrator

import org.jetbrains.exposed.sql.Database

abstract class Migration {
    abstract fun migrate(db: Database)
    abstract fun generateRollbackStatements(db: Database): List<String>
}
