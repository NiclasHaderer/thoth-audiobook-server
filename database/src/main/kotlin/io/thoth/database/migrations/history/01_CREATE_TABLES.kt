package io.thoth.database.migrations.history

import io.thoth.database.migrations.migrator.Migration
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime


class `01_Create_Tables` : Migration() {
    override fun migrate(db: Database) {
        transaction {
            SchemaUtils.create(
                TAuthors,
                TBooks,
                TImages,
                TProviderID,
                TSeries,
                TKeyValueSettings,
                TTracks,
            )
        }
    }

    override fun rollback(db: Database) { // Nothing to do
    }

}

object TAuthors : UUIDTable("Authors") {
    val name = varchar("name", 255).uniqueIndex()
    val biography = text("biography").nullable()
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val providerID = reference("providerID", TProviderID).nullable()
    val image = reference("image", TImages).nullable()
}

object TBooks : UUIDTable("Books") {
    val title = varchar("title", 255)
    val author = reference("author", TAuthors)
    val year = integer("year").nullable()
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val providerID = reference("providerID", TProviderID).nullable()
    val narrator = varchar("name", 255).nullable()
    val series = reference("series", TSeries).nullable()
    val seriesIndex = float("seriesIndex").nullable()
    val cover = reference("cover", TImages).nullable()
}

object TImages : UUIDTable("Images") {
    val image = blob("image")
}

object TKeyValueSettings : UUIDTable("KeyValueSettings") {
    val scanIndex = long("scanIndex").default(0)
}

object TProviderID : UUIDTable("ProviderID") {
    val provider = char("provider", 40)
    val itemID = char("bookID", 40)
}

object TSeries : UUIDTable("Series") {
    val title = varchar("title", 250).uniqueIndex()
    val author = reference("author", TAuthors)
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val providerID = reference("providerID", TProviderID).nullable()
    val description = text("description").nullable()
}

object TTracks : UUIDTable("Tracks") {
    val title = varchar("title", 255)
    val duration = integer("duration")
    val accessTime = long("accessTime")
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val path = text("path").uniqueIndex()
    val book = reference("book", TBooks)
    val scanIndex = long("scanIndex")
    val trackNr = integer("trackNr").nullable()
}
