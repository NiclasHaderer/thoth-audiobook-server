package io.thoth.generators

import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSeries
import io.thoth.models.AuthorModel
import io.thoth.models.BookModel
import io.thoth.models.DetailedAuthorModel
import io.thoth.models.DetailedBookModel
import io.thoth.models.DetailedSeriesModel
import io.thoth.models.NamedId
import io.thoth.models.PaginatedResponse
import io.thoth.models.SearchModel
import io.thoth.models.SeriesModel
import io.thoth.models.TrackModel
import io.thoth.server.api.audiobooks.library.authors.PatchAuthor
import io.thoth.server.api.audiobooks.library.authors.PostAuthor
import io.thoth.server.api.audiobooks.library.books.PatchBook
import io.thoth.server.api.audiobooks.library.books.PostBook
import io.thoth.server.api.audiobooks.library.series.PatchSeries
import io.thoth.server.api.audiobooks.library.series.PostSeries
import io.thoth.server.ws.ChangeEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass
import me.ntrrgc.tsGenerator.TypeScriptGenerator

fun main() {
    var audiobookDefinitions =
        generate(
            setOf(
                AuthorModel::class,
                DetailedAuthorModel::class,
                BookModel::class,
                DetailedBookModel::class,
                SearchModel::class,
                SeriesModel::class,
                PatchAuthor::class,
                PatchSeries::class,
                PatchBook::class,
                PostAuthor::class,
                PostSeries::class,
                PostBook::class,
                DetailedSeriesModel::class,
                TrackModel::class,
                ChangeEvent::class,
                PaginatedResponse::class,
                NamedId::class,
            ),
        )

    audiobookDefinitions = audiobookDefinitions.replace("interface", "export interface")

    var metadataDefinitions =
        generate(setOf(MetadataAuthor::class, MetadataBook::class, MetadataSearchBook::class, MetadataSeries::class))
    metadataDefinitions = metadataDefinitions.replace("interface", "export interface")

    if (!Files.exists(Path.of("gen/typescript"))) {
        Files.createDirectories(Path.of("gen/typescript"))
    }

    File("gen/typescript/api.ts").printWriter().use { out -> out.println(audiobookDefinitions) }
    File("gen/typescript/metadata.ts").printWriter().use { out -> out.println(metadataDefinitions) }
}

fun generate(classes: Iterable<KClass<*>>) =
    TypeScriptGenerator(
            rootClasses = classes,
            mappings =
                mapOf(
                    LocalDateTime::class to "number",
                    LocalDate::class to "number",
                    Date::class to "number",
                    UUID::class to "string",
                ),
        )
        .definitionsText
