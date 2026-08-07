package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime

// TODO make sure that two libraries do not cover the same paths, otherwise the path reference will
// not be unique
object TracksTable : UUIDTable("Tracks") {
    val title = text("title")
    val duration = integer("duration")
    val accessTime = long("accessTime")
    // A literal default would freeze the timestamp at the moment the table was created, so it has to be an expression
    val updateTime = datetime("updateTime").defaultExpression(CurrentDateTime)
    val path = text("path").uniqueIndex()
    val book = reference("book", BooksTable, onDelete = ReferenceOption.CASCADE).index()
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE).index()
    val scanIndex = ulong("scanIndex")
    val trackNr = integer("trackNr").nullable()
}
