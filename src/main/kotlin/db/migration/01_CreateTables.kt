package db.migration


import io.github.huiibuh.db.migration.Migration
import io.github.huiibuh.db.models.Albums
import io.github.huiibuh.db.models.Artists
import io.github.huiibuh.db.models.Collections
import io.github.huiibuh.db.models.Tracks
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class `01_CreateTables` : Migration() {
    override fun migrate(db: Database) {
//        transaction {
//            SchemaUtils.create(Albums)
//            SchemaUtils.create(Tracks)
//            SchemaUtils.create(Collections)
//            SchemaUtils.create(Artists)
//        }
    }


    override fun rollback(db: Database) = Unit
}


