package io.thoth.server

import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.AuthorFileMetadataTable
import io.thoth.server.database.tables.AuthorMetadataRow
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.AuthorUserMetadataTable
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.BookMetadataRow
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.LibraryRow
import io.thoth.server.database.tables.replaceBookAuthors
import io.thoth.server.database.tables.SeriesFileMetadataTable
import io.thoth.server.database.tables.SeriesMetadataRow
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackRow
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.replaceBookSeries
import io.thoth.server.database.tables.write
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun newLibrary(
    name: String,
    folders: List<String> = listOf("/media/$name"),
    scanIndex: ULong = 0uL,
    fileScanners: List<FileScanner> = listOf(FileScanner("AudioFolderScanner")),
    preferEmbeddedMetadata: Boolean = false,
): UUID =
    transaction {
        LibrariesTable.insert(
            LibraryRow(
                id = UUID.randomUUID(),
                name = name,
                icon = null,
                scanIndex = scanIndex,
                folders = folders,
                preferEmbeddedMetadata = preferEmbeddedMetadata,
                metadataAgents = listOf(NamedMetadataAgent("audible")),
                fileScanners = fileScanners,
                language = "en",
            ),
        )
    }

fun newAuthor(
    name: String,
    libraryId: UUID,
    renamedTo: String? = null,
): UUID =
    transaction {
        val id = AuthorTable.create(libraryId)
        AuthorFileMetadataTable.write(AuthorMetadataRow(author = id, name = name))
        if (renamedTo != null) {
            AuthorUserMetadataTable.write(AuthorMetadataRow(author = id, name = renamedTo))
        }
        id
    }

fun newSeries(
    title: String,
    libraryId: UUID,
): UUID =
    transaction {
        val id = SeriesTable.create(libraryId)
        SeriesFileMetadataTable.write(SeriesMetadataRow(series = id, title = title))
        id
    }

fun newBook(
    title: String,
    libraryId: UUID,
    authors: List<UUID> = emptyList(),
    series: List<UUID> = emptyList(),
    narrators: List<String>? = null,
    genres: List<String>? = null,
): UUID =
    transaction {
        val id = BooksTable.create(libraryId)
        BookFileMetadataTable.write(
            BookMetadataRow(book = id, title = title, narrators = narrators, genres = genres),
        )
        replaceBookAuthors(id, MetadataLayer.FILE, authors)
        replaceBookSeries(id, MetadataLayer.FILE, series.associateWith { null })
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
