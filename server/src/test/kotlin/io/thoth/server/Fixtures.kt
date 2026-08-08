package io.thoth.server

import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorRow
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookRow
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.LibraryRow
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesRow
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackRow
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.replaceLinks
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun newLibrary(
    name: String,
    folders: List<String> = listOf("/media/$name"),
    scanIndex: ULong = 0uL,
    fileScanners: List<FileScanner> = listOf(FileScanner("AudioFolderScanner")),
): UUID =
    transaction {
        LibrariesTable.insert(
            LibraryRow(
                id = UUID.randomUUID(),
                name = name,
                icon = null,
                scanIndex = scanIndex,
                folders = folders,
                preferEmbeddedMetadata = false,
                metadataAgents = listOf(NamedMetadataAgent("audible")),
                fileScanners = fileScanners,
                language = "en",
            ),
        )
    }

fun newAuthor(
    name: String,
    libraryId: UUID,
): UUID =
    transaction {
        AuthorTable.insert(
            AuthorRow(
                id = UUID.randomUUID(),
                name = name,
                displayName = null,
                biography = null,
                website = null,
                birthDate = null,
                bornIn = null,
                deathDate = null,
                provider = null,
                providerID = null,
                imageID = null,
                library = libraryId,
            ),
        )
    }

fun newSeries(
    title: String,
    libraryId: UUID,
): UUID =
    transaction {
        SeriesTable.insert(
            SeriesRow(
                id = UUID.randomUUID(),
                title = title,
                displayTitle = null,
                totalBooks = null,
                primaryWorks = null,
                description = null,
                provider = null,
                providerID = null,
                coverID = null,
                library = libraryId,
            ),
        )
    }

fun newBook(
    title: String,
    libraryId: UUID,
    authors: List<UUID> = emptyList(),
    series: List<UUID> = emptyList(),
): UUID =
    transaction {
        val id =
            BooksTable.insert(
                BookRow(
                    id = UUID.randomUUID(),
                    title = title,
                    displayTitle = null,
                    releaseDate = null,
                    publisher = null,
                    language = null,
                    description = null,
                    narrator = null,
                    isbn = null,
                    provider = null,
                    providerID = null,
                    providerRating = null,
                    coverID = null,
                    library = libraryId,
                ),
            )
        AuthorBookTable.replaceLinks(AuthorBookTable.book, id, AuthorBookTable.authors, authors)
        SeriesBookTable.replaceLinks(SeriesBookTable.book, id, SeriesBookTable.series, series)
        id
    }

fun newTrack(
    title: String,
    path: String,
    bookId: UUID,
    libraryId: UUID,
    fileModifiedAt: Long = 0,
    scanIndex: ULong = 0uL,
    trackNr: Int? = null,
): UUID =
    transaction {
        TracksTable.insert(
            TrackRow(
                id = UUID.randomUUID(),
                title = title,
                duration = 60,
                fileModifiedAt = fileModifiedAt,
                path = path,
                book = bookId,
                library = libraryId,
                scanIndex = scanIndex,
                trackNr = trackNr,
            ),
        )
    }
