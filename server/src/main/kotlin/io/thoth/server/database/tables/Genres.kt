package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

object TGenres : UUIDTable("Genres") {
    val name = varchar("name", 255)
}

class Genre(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Genre>(TGenres)

    var name by TGenres.name
    val books by Book via TGenreBookMapping
    val series by Series via TGenreSeriesMapping
}
