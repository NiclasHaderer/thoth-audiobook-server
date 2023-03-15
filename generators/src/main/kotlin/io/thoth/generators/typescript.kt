package io.thoth.generators

import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSeries
import io.thoth.models.PaginatedResponse
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
                PaginatedResponse::class,
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
