package io.thoth.database.tables

import io.thoth.common.exceptions.APINotFound
import io.thoth.common.extensions.findOne
import io.thoth.database.ToModel
import io.thoth.models.BookModel
import io.thoth.models.BookModelWithTracks
import io.thoth.models.NamedId
import io.thoth.models.TitledId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*


object TBooks : UUIDTable("Books") {
    val title = varchar("title", 255)
    val author = reference("author", TAuthors)
    val year = integer("year").nullable()
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val providerID = reference("providerID", TProviderID).nullable()
    val narrator = varchar("name", 255).nullable()
    val series = reference("series", TSeries).nullable()
    val seriesIndex = float("seriesIndex").nullable()
    val cover = reference("cover", TImages).nullable()
}

class Book(id: EntityID<UUID>) : UUIDEntity(id), ToModel<BookModel> {
    companion object : UUIDEntityClass<Book>(TBooks) {
        fun removeUnused() = transaction {
            all().forEach {
                if (Track.find { TTracks.book eq it.id.value }.empty()) {
                    it.delete()
                }
            }
        }

        fun getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC) = transaction {
            Book.all().limit(limit, offset * limit).orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
        }

        fun fromAuthor(authorID: UUID, order: SortOrder = SortOrder.ASC) = transaction {
            find { TBooks.author eq authorID }.orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
        }

        fun totalCount() = transaction { Book.all().count() }

        fun getByName(bookTitle: String, author: Author) = transaction {
            Book.findOne { TBooks.title like bookTitle and (TBooks.author eq author.id.value) }
        }

        @Throws(APINotFound::class)
        fun getById(uuid: UUID, order: SortOrder = SortOrder.ASC) = transaction {
            val book = Book.findById(uuid)?.toModel() ?: throw APINotFound("Could not find album")
            val tracks = Track.forBook(uuid)
            val sortPosition = Book.all().orderBy(TBooks.title.lowerCase() to order).toList().count()
            BookModelWithTracks.fromModel(book, tracks, sortPosition)
        }

        fun forSeries(seriesId: UUID, order: SortOrder = SortOrder.ASC) = transaction {
            Book.find { TBooks.series eq seriesId }.orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
        }

    }

    private val coverID by TBooks.cover

    var title by TBooks.title
    var year by TBooks.year
    var language by TBooks.language
    var description by TBooks.description
    var updateTime by TBooks.updateTime
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
        updateTime = updateTime,
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
