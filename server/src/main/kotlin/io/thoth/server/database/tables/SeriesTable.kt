package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption

object SeriesTable : LibraryEntityTable("Series", "title") {
    val title get() = name
    val totalBooks = integer("totalBooks").nullable()
    val primaryWorks = integer("primaryWorks").nullable()
    val description = text("description").nullable()
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val coverID = reference("cover", ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
}
