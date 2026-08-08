package io.thoth.server.api

import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.routing.Routing
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.responses.BinaryResponse
import io.thoth.openapi.ktor.responses.FileResponse
import io.thoth.openapi.ktor.responses.binaryResponse
import io.thoth.openapi.ktor.responses.fileResponse
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.plugins.auth.assertLibraryPermissions
import io.thoth.server.plugins.auth.thothPrincipal
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

fun Routing.audioRouting() {
    get<Api.Files.Audio.Id, FileResponse> { (id) ->
        val (track, libraryId) =
            transaction {
                val row =
                    TracksTable
                        .select(TracksTable.path, TracksTable.library)
                        .where { TracksTable.id eq id }
                        .firstOrNull()
                        ?: throw ErrorResponse.notFound("Track", id)
                row[TracksTable.path] to row[TracksTable.library].value
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
        fileResponse(path)
    }
}

fun Routing.imageRouting() {
    get<Api.Files.Images.Id, BinaryResponse> { (id) ->
        val permissions = thothPrincipal().permissions
        transaction {
            val image =
                ImageTable.selectAll().where { ImageTable.id eq id }.firstOrNull()
                    ?: throw ErrorResponse.notFound("Image", id)
            if (!permissions.isAdmin) {
                val allowed = permissions.libraries.mapTo(mutableSetOf()) { it.id }
                val owningLibraries =
                    BooksTable
                        .select(BooksTable.library)
                        .where { BooksTable.coverID eq id }
                        .map { it[BooksTable.library].value } +
                        AuthorTable
                            .select(AuthorTable.library)
                            .where { AuthorTable.imageID eq id }
                            .map { it[AuthorTable.library].value } +
                        SeriesTable
                            .select(SeriesTable.library)
                            .where { SeriesTable.coverID eq id }
                            .map { it[SeriesTable.library].value }
                if (owningLibraries.none { it in allowed }) {
                    throw ErrorResponse.forbidden("access", "Image $id")
                }
            }
            binaryResponse(image[ImageTable.blob].bytes)
        }
    }
}
