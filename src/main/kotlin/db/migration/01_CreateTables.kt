package db.migration


import io.github.huiibuh.db.migration.Migration
import org.jetbrains.exposed.sql.Database

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


