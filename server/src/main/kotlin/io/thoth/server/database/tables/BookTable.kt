package io.thoth.server.database.tables

import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.models.ChapterMark
import io.thoth.server.database.extensions.json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.date

object BookTable : LibraryEntityTable("book", BookField.TITLE.column) {
    val title get() = name
    val releaseDate = date(BookField.RELEASE_DATE.column).nullable()
    val publisher = varchar(BookField.PUBLISHER.column, 255).nullable()
    val language = enumerationByName<MetadataLanguage>(BookField.LANGUAGE.column, 255).nullable()
    val description = text(BookField.DESCRIPTION.column).nullable()
    val isbn = varchar(BookField.ISBN.column, 255).nullable()
    val provider = varchar(BookField.PROVIDER.column, 255).nullable()
    val providerId = varchar(BookField.PROVIDER_ID.column, 255).nullable()
    val providerRating = float(BookField.PROVIDER_RATING.column).nullable()
    val coverId = reference(BookField.COVER_ID.column, ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val genres = json<List<String>>(BookField.GENRES.column).nullable()
    val narrators = json<List<String>>(BookField.NARRATORS.column).nullable()
    val chapters = json<List<ChapterMark>>(BookField.CHAPTERS.column).nullable()

    val authorsFrom = enumerationByName<MetadataLayer>("authors_from", 8).default(MetadataLayer.FILE)
    val seriesFrom = enumerationByName<MetadataLayer>("series_from", 8).default(MetadataLayer.FILE)
}
