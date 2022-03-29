import io.github.huiibuh.api.audiobooks.authors.PatchAuthor
import io.github.huiibuh.api.audiobooks.books.PatchBook
import io.github.huiibuh.api.audiobooks.series.PatchSeries
import io.github.huiibuh.metadata.AuthorMetadata
import io.github.huiibuh.metadata.BookMetadata
import io.github.huiibuh.metadata.SearchBookMetadata
import io.github.huiibuh.metadata.SeriesMetadata
import io.github.huiibuh.models.AuthorModel
import io.github.huiibuh.models.AuthorModelWithBooks
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.models.BookModelWithTracks
import io.github.huiibuh.models.PaginatedResponse
import io.github.huiibuh.models.SearchModel
import io.github.huiibuh.models.SeriesModel
import io.github.huiibuh.models.SeriesModelWithBooks
import io.github.huiibuh.models.TrackModel
import io.github.huiibuh.ws.ChangeEvent
import me.ntrrgc.tsGenerator.TypeScriptGenerator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
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
