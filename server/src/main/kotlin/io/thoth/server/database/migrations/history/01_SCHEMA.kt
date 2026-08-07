package io.thoth.server.database.migrations.history

import io.thoth.server.database.THOTH_TABLES
import io.thoth.server.database.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class `01_SCHEMA` : Migration() {
    override fun migrate() {
        transaction {
            // TODO once we do migrations properly, we need to copy over the tables in to the migration file to make
            //  them immutable
            SchemaUtils.create(*THOTH_TABLES)
        }
    }
}
