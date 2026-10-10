package io.thoth.server.api

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.routing.Routing
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.responses.BinaryResponse
import io.thoth.openapi.ktor.responses.FileResponse
import io.thoth.openapi.ktor.responses.binaryResponse
import io.thoth.openapi.ktor.responses.fileResponse
import io.thoth.server.common.audioContentType
import io.thoth.server.common.imageContentType
import io.thoth.server.database.tables.AuthorAgentMetadataTable
import io.thoth.server.database.tables.AuthorFileMetadataTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.AuthorUserMetadataTable
import io.thoth.server.database.tables.BookAgentMetadataTable
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.SeriesAgentMetadataTable
import io.thoth.server.database.tables.SeriesFileMetadataTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.SeriesUserMetadataTable
import io.thoth.server.database.tables.TrackTable
import io.thoth.server.plugins.auth.assertLibraryPermissions
import io.thoth.server.plugins.auth.thothPrincipal
import io.thoth.server.plugins.sandbox
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.unionAll
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

fun Routing.audioRouting() {
    get<Api.Files.Audio.Id, FileResponse> { (id) ->
        val (track, libraryId) =
            transaction {
                val row =
                    TrackTable
                        .select(TrackTable.path, TrackTable.library)
                        .where { TrackTable.id eq id }
                        .firstOrNull()
                        ?: throw ErrorResponse.notFound("Track", id)
                row[TrackTable.path] to row[TrackTable.library].value
            }
        assertLibraryPermissions(libraryId)
        val path = Path.of(track)
        if (!path.exists() || !path.isRegularFile()) {
            throw ErrorResponse.notFound("File", path.name, "Database out of sync. Please start a rescan.")
        }
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, path.name).toString(),
        )
        call.sandbox()
        fileResponse(path, audioContentType(path))
    }
}

fun Routing.imageRouting() {
    get<Api.Files.Images.Id, BinaryResponse> { (id) ->
        val permissions = thothPrincipal().permissions
        call.sandbox()
        transaction {
            val allowed = permissions.libraries.mapTo(mutableSetOf()) { it.id }
            val covers =
                listOf(BookFileMetadataTable, BookAgentMetadataTable, BookUserMetadataTable)
                    .map { cover(BookTable, BookTable.library, it, it.coverId, id, allowed) } +
                    listOf(SeriesFileMetadataTable, SeriesAgentMetadataTable, SeriesUserMetadataTable)
                        .map { cover(SeriesTable, SeriesTable.library, it, it.coverId, id, allowed) } +
                    listOf(AuthorFileMetadataTable, AuthorAgentMetadataTable, AuthorUserMetadataTable)
                        .map { cover(AuthorTable, AuthorTable.library, it, it.imageId, id, allowed) }
            val image =
                covers
                    .reduce { acc: AbstractQuery<*>, query -> acc.unionAll(query) }
                    .limit(1)
                    .firstOrNull() ?: throw ErrorResponse.notFound("Image", id)
            val bytes = image[ImageTable.image].bytes
            binaryResponse(
                bytes,
                contentType = imageContentType(bytes) ?: ContentType.Application.OctetStream,
                // A new row is created whenever the bytes change, so an id always maps to the same image
                cacheControl = "private, max-age=$IMAGE_MAX_AGE_SECONDS, immutable",
                etag = id.toString(),
            )
        }
    }
}

private const val IMAGE_MAX_AGE_SECONDS = 365 * 24 * 60 * 60

private fun cover(
    core: Table,
    library: Column<EntityID<UUID>>,
    layer: Table,
    image: Column<EntityID<UUID>?>,
    imageId: UUID,
    allowed: Set<UUID>,
): Query =
    (core innerJoin layer)
        .join(ImageTable, JoinType.INNER, image, ImageTable.id)
        .select(ImageTable.image)
        .where { (image eq imageId) and (library inList allowed) }
