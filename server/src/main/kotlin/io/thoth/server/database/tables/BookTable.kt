package io.thoth.server.database.tables

import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.models.ChapterMark
import io.thoth.server.database.extensions.json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.date

enum class BookField : MetadataField {
    TITLE,
    AUTHORS,
    SERIES,
    PROVIDER,
    PROVIDER_ID,
    PROVIDER_RATING,
    RELEASE_DATE,
    PUBLISHER,
    LANGUAGE,
    DESCRIPTION,
    NARRATORS,
    GENRES,
    ISBN,
    COVER_ID,
    CHAPTERS,
}

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

    // Null while nobody said anything about the chapters: they are then derived from the tracks, which a scan
    // never writes down here because they depend on all tracks of the book
    val chapters = json<List<ChapterMark>>(BookField.CHAPTERS.column).nullable()
    val locked = json<Set<BookField>>("locked").default(emptySet())
}
