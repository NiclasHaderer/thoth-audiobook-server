package io.github.huiibuh.db.tables

import io.github.huiibuh.db.ToModel
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.models.NamedId
import io.github.huiibuh.models.TitledId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


object TBooks : UUIDTable("Books") {
    val title = varchar("title", 255)
    val year = integer("year").nullable()
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val author = reference("author", TAuthors)
    val providerID = reference("providerID", TProviderID).nullable()
    val narrator = varchar("name", 255).nullable()
    val series = reference("series", TSeries).nullable()
    val seriesIndex = float("seriesIndex").nullable()
    val cover = reference("cover", TImages).nullable()
}

class Book(id: EntityID<UUID>) : UUIDEntity(id), ToModel<BookModel> {
    companion object : UUIDEntityClass<Book>(TBooks)

    private val coverID by TBooks.cover

    var title by TBooks.title
    var year by TBooks.year
    var language by TBooks.language
    var description by TBooks.description
    var providerID by ProviderID optionalReferencedOn TBooks.providerID
    var author by Author referencedOn TBooks.author
    var narrator by TBooks.narrator
    var series by Series optionalReferencedOn TBooks.series
    var seriesIndex by TBooks.seriesIndex
    var cover by Image optionalReferencedOn TBooks.cover

    override fun toModel() = BookModel(
        id = id.value,
        title = title,
        year = year,
        language = language,
        description = description,
        providerID = providerID?.toModel(),
        author = NamedId(
            name = author.name,
            id = author.id.value
        ),
        narrator = narrator,
        series = if (series != null) TitledId(
            title = series!!.title,
            id = series!!.id.value
        ) else null,
        seriesIndex = seriesIndex,
        cover = coverID?.value
    )
}
