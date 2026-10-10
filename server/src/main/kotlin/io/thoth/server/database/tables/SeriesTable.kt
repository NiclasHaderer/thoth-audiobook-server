package io.thoth.server.database.tables

import io.thoth.server.database.extensions.json
import org.jetbrains.exposed.v1.core.ReferenceOption

enum class SeriesField : MetadataField {
    TITLE,
    PROVIDER,
    PROVIDER_ID,
    TOTAL_BOOKS,
    PRIMARY_WORKS,
    COVER_ID,
    DESCRIPTION,
}

object SeriesTable : LibraryEntityTable("series", SeriesField.TITLE.column) {
    val title get() = name
    val totalBooks = integer(SeriesField.TOTAL_BOOKS.column).nullable()
    val primaryWorks = integer(SeriesField.PRIMARY_WORKS.column).nullable()
    val description = text(SeriesField.DESCRIPTION.column).nullable()
    val provider = varchar(SeriesField.PROVIDER.column, 255).nullable()
    val providerId = varchar(SeriesField.PROVIDER_ID.column, 255).nullable()
    val coverId = reference(SeriesField.COVER_ID.column, ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val locked = json<Set<SeriesField>>("locked").default(emptySet())
}
