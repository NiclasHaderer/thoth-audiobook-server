package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

object GenresTable : UUIDTable("Genres") {
    val name = varchar("name", 255)
}

class GenreEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<GenreEntity>(GenresTable)

    var name by GenresTable.name
    val books by BookeEntity via GenreBookTable
    val series by SeriesEntity via GenreSeriesTable
}
