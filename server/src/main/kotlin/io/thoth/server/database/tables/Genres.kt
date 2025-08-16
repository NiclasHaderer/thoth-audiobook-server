package io.thoth.server.database.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
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
