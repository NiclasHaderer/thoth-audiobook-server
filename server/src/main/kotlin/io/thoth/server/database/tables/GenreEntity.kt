package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.UUID

class GenreEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<GenreEntity>(GenresTable)

    var name by GenresTable.name
    val books by BookEntity via GenreBookTable
    val series by SeriesEntity via GenreSeriesTable
}
