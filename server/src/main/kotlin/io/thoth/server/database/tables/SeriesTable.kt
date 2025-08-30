package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object SeriesTable : UUIDTable("Series") {
    val title = varchar("title", 255)
    val displayTitle = varchar("displayTitle", 255).nullable()
    val totalBooks = integer("totalBooks").nullable()
    val primaryWorks = integer("primaryWorks").nullable()
    val description = text("description").nullable()

    // Provider
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()

    // Relations
    val coverID = reference("cover", ImageTable).nullable()
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE)
}
