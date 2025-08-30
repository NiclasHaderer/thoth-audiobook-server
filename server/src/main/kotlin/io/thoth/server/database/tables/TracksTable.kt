package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

// TODO make sure that two libraries do not cover the same paths, otherwise the path reference will
// not be unique
object TracksTable : UUIDTable("Tracks") {
    val title = varchar("title", 255)
    val duration = integer("duration")
    val accessTime = long("accessTime")
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val path = text("path").uniqueIndex()
    val book = reference("book", BooksTable)
    val scanIndex = ulong("scanIndex")
    val trackNr = integer("trackNr").nullable()
}
