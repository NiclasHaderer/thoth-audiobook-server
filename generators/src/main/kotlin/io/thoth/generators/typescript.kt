package io.thoth.generators


import io.thoth.metadata.AuthorMetadata
import io.thoth.metadata.BookMetadata
import io.thoth.metadata.SearchBookMetadata
import io.thoth.metadata.SeriesMetadata
import io.thoth.models.*
import io.thoth.server.api.audiobooks.authors.PatchAuthor
import io.thoth.server.api.audiobooks.books.PatchBook
import io.thoth.server.api.audiobooks.series.PatchSeries
import me.ntrrgc.tsGenerator.TypeScriptGenerator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.swing.event.ChangeEvent
import kotlin.reflect.KClass

fun main() {
    var audiobookDefinitions = generate(
        setOf(
            AuthorModel::class,
            AuthorModelWithBooks::class,
            BookModel::class,
            BookModelWithTracks::class,
            SearchModel::class,
            SeriesModel::class,
            PatchAuthor::class,
            PatchSeries::class,
            PatchBook::class,
            SeriesModelWithBooks::class,
            TrackModel::class,
            ChangeEvent::class,
            PaginatedResponse::class
        )
    )

    audiobookDefinitions = audiobookDefinitions.replace("interface", "export interface")

    var audibleDefinitions = generate(
        setOf(
            AuthorMetadata::class,
            BookMetadata::class,
            SearchBookMetadata::class,
            SeriesMetadata::class
        )
    )
    audibleDefinitions = audibleDefinitions.replace("interface", "export interface")

    if (!Files.exists(Path.of("gen/typescript"))) {
        Files.createDirectories(Path.of("gen/typescript"))
    }

    File("gen/typescript/audiobook.ts").printWriter().use { out ->
        out.println(audiobookDefinitions)
    }
    File("gen/typescript/audible.ts").printWriter().use { out ->
        out.println(audibleDefinitions)
    }

}


fun generate(classes: Iterable<KClass<*>>) = TypeScriptGenerator(
    rootClasses = classes,
    mappings = mapOf(
        LocalDateTime::class to "number",
        LocalDate::class to "number",
        Date::class to "number",
        UUID::class to "string",
    )
).definitionsText
