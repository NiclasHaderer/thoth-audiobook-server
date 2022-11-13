package io.thoth.database.tables.meta

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object TMetaGenres : UUIDTable("MetaGenres") {
    val name = varchar("name", 255)
}

class MetaGenre(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MetaGenre>(TMetaGenres)

    var name by TMetaGenres.name
    val books by MetaBook via TMetaGenreBookMapping
    val series by MetaSeries via TMetaGenreSeriesMapping
    val authors by MetaAuthor via TMetaGenreAuthorMapping
}
