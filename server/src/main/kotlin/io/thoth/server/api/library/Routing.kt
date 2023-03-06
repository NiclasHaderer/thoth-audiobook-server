package io.thoth.server.api.library

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.access.foldersOverlap
import io.thoth.database.access.toModel
import io.thoth.database.tables.Library
import io.thoth.models.LibraryModel
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.post
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.registerLibraryRouting() =
    route("library") {
        get<Unit, List<LibraryModel>> { transaction { Library.all().map { it.toModel() } } }

        get<LibraryId, LibraryModel> { (id) ->
            transaction {
                Library.findById(id)?.toModel() ?: serverError(HttpStatusCode.NotFound, "Library was not found")
            }
        }

        post<LibraryId, PostLibrary, LibraryModel> { (id), postLibrary ->
            transaction {
                val (overlaps, overlapping) = Library.foldersOverlap(postLibrary.folders)
                if (!overlaps)
                    serverError(
                        HttpStatusCode.Conflict,
                        "Folders overlap with existing libraries",
                        "overlaps" to overlapping,
                    )

                val library = Library.findById(id) ?: Library.new { name = postLibrary.name }
                library.apply {
                    name = postLibrary.name
                    icon = postLibrary.icon
                    folders = postLibrary.folders
                    preferEmbeddedMetadata = postLibrary.preferEmbeddedMetadata
                }
                library.toModel()
            }
        }

        patch<LibraryId, PatchLibrary, LibraryModel> { (id), patchLibrary ->
            transaction {
                if (patchLibrary.folders != null) {
                    val (overlaps, overlapping) = Library.foldersOverlap(patchLibrary.folders)
                    if (!overlaps)
                        serverError(
                            HttpStatusCode.Conflict,
                            "Folders overlap with existing libraries",
                            "overlaps" to overlapping,
                        )
                }

                val library = Library.findById(id) ?: serverError(HttpStatusCode.NotFound, "Library was not found")
                library.apply {
                    name = patchLibrary.name ?: name
                    icon = patchLibrary.icon ?: icon
                    folders = patchLibrary.folders ?: folders
                    preferEmbeddedMetadata = patchLibrary.preferEmbeddedMetadata ?: preferEmbeddedMetadata
                }
                library.toModel()
            }
        }
    }
