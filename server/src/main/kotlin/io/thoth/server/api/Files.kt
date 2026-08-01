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
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageEntity
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.plugins.auth.assertLibraryPermissions
import io.thoth.server.plugins.auth.thothPrincipal
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.core.*
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

fun Routing.audioRouting() {
    get<Api.Files.Audio.Id, FileResponse> { (id) ->
        val (track, libraryId) =
            transaction {
                val track = TrackEntity.findById(id) ?: throw ErrorResponse.notFound("Track", id)
                track.path to track.library.id.value
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
            val image = ImageEntity.findById(id) ?: throw ErrorResponse.notFound("Image", id)
            if (!permissions.isAdmin) {
                val allowed = permissions.libraries.mapTo(mutableSetOf()) { it.id }
                val owningLibraries =
                    BookEntity.find { BooksTable.coverID eq id }.map { it.library.id.value } +
                        AuthorEntity.find { AuthorTable.imageID eq id }.map { it.library.id.value } +
                        SeriesEntity.find { SeriesTable.coverID eq id }.map { it.library.id.value }
                if (owningLibraries.none { it in allowed }) {
                    throw ErrorResponse.forbidden("access", "Image $id")
                }
            }
            binaryResponse(image.blob.bytes)
        }
    }
}
