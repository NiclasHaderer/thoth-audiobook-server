package codeGenerator

import audible.models.AudibleAuthor
import audible.models.AudibleBook
import audible.models.AudibleSearchResultImpl
import audible.models.AudibleSeries
import io.github.huiibuh.models.AuthorModel
import io.github.huiibuh.models.AuthorModelWithBooks
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.models.BookModelWithTracks
import io.github.huiibuh.models.SearchModel
import io.github.huiibuh.models.SeriesModel
import io.github.huiibuh.models.SeriesModelWithBooks
import io.github.huiibuh.models.TrackModel
import me.ntrrgc.tsGenerator.TypeScriptGenerator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

fun main() {
    var audiobookDefinitions = generate(setOf(
        AuthorModel::class,
        AuthorModelWithBooks::class,
        BookModel::class,
        BookModelWithTracks::class,
        SearchModel::class,
        SearchModel::class,
        SeriesModel::class,
        SeriesModelWithBooks::class,
        TrackModel::class,
    ))

    audiobookDefinitions = audiobookDefinitions.replace("interface", "export interface")

    var audibleDefinitions = generate(setOf(
        AudibleAuthor::class,
        AudibleBook::class,
        AudibleSearchResultImpl::class,
        AudibleSeries::class
    ))
    audibleDefinitions = audibleDefinitions.replace("interface", "export interface")

    if (!Files.exists(Path.of("gen"))) {
        Files.createDirectories(Path.of("gen"))
    }

    File("gen/audiobook.ts").printWriter().use { out ->
        out.println(audiobookDefinitions)
    }
    File("gen/audible.ts").printWriter().use { out ->
        out.println(audibleDefinitions)
    }

}


fun generate(classes: Iterable<KClass<*>>) = TypeScriptGenerator(
    rootClasses = classes,
    mappings = mapOf(
        LocalDateTime::class to "Date",
        LocalDate::class to "Date",
        Date::class to "Date",
        UUID::class to "string"
    )
).definitionsText
