package io.thoth.server.database.tables

import io.thoth.models.LibraryPermissions
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass

object TAuthorBookMapping : CompositeIdTable("AuthorBookMapping") {
    val authors = reference("author", TAuthors, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", TBooks, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(authors, book)
}

object TGenreBookMapping : CompositeIdTable("GenreBookMapping") {
    val genre = reference("genre", TGenres, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", TBooks, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(genre, book)
}

object TGenreSeriesMapping : CompositeIdTable("GenreSeriesMapping") {
    val genre = reference("genre", TGenres, onDelete = ReferenceOption.CASCADE)
    val series = reference("Series", TSeries, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(genre, series)
}

object TSeriesBookMapping : CompositeIdTable("SeriesBookMapping") {
    val series = reference("series", TSeries, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", TBooks, onDelete = ReferenceOption.CASCADE)
    val seriesIndex = float("index").nullable()
    override val primaryKey = PrimaryKey(series, book)
}

object TSeriesAuthorMapping : CompositeIdTable("SeriesAuthorMapping") {
    val series = reference("series", TSeries, onDelete = ReferenceOption.CASCADE)
    val author = reference("author", TAuthors, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(series, author)
}

object TLibraryUserMapping : CompositeIdTable("LibraryUserMapping") {
    val library = reference("library", TLibraries, onDelete = ReferenceOption.CASCADE)
    val user = reference("user", TUsers, onDelete = ReferenceOption.CASCADE)
    var permissions = enumeration<LibraryPermissions>("permissions")
    override val primaryKey = PrimaryKey(library, user)
}

class LibraryUserMappingEntity(
    id: EntityID<CompositeID>,
) : CompositeEntity(id) {
    companion object : CompositeEntityClass<LibraryUserMappingEntity>(TLibraryUserMapping)

    var permissions by TLibraryUserMapping.permissions
    var library by Library referencedOn TLibraryUserMapping.library
    var user by User referencedOn TLibraryUserMapping.user
}
