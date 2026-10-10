package io.thoth.server

import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.models.FileScanner
import io.thoth.models.LibraryPermissionLevel
import io.thoth.models.NamedMetadataAgent
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.LibraryRow
import io.thoth.server.database.tables.LibraryTable
import io.thoth.server.database.tables.LibraryUserTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackChapter
import io.thoth.server.database.tables.TrackRow
import io.thoth.server.database.tables.TrackTable
import io.thoth.server.database.tables.UserRow
import io.thoth.server.database.tables.UserTable
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.replaceBookAuthors
import io.thoth.server.database.tables.replaceBookSeries
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.UUID

// Cover art is only stored if it sniffs as a real image, so test art needs a valid header
fun pngBytes(vararg payload: Byte): ByteArray =
    byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + payload

fun newLibrary(
    name: String,
    folders: List<String> = listOf("/media/$name"),
    scanIndex: ULong = 0uL,
    fileScanners: List<FileScanner> = listOf(FileScanner("AudioFolderScanner")),
    combineFileScannerFields: Boolean = true,
    preferEmbeddedMetadata: Boolean = false,
    region: MetadataRegion = MetadataRegion.US,
    language: MetadataLanguage = MetadataLanguage.English,
    metadataAgents: List<NamedMetadataAgent> = listOf(NamedMetadataAgent("audible")),
    combineMetadataAgentFields: Boolean = true,
): UUID =
    transaction {
        LibraryTable.insert(
            LibraryRow(
                id = UUID.randomUUID(),
                name = name,
                icon = null,
                scanIndex = scanIndex,
                folders = folders,
                preferEmbeddedMetadata = preferEmbeddedMetadata,
                metadataAgents = metadataAgents,
                combineMetadataAgentFields = combineMetadataAgentFields,
                fileScanners = fileScanners,
                combineFileScannerFields = combineFileScannerFields,
                language = language,
                region = region,
            ),
        )
    }

fun newAuthor(
    name: String,
    libraryId: UUID,
    renamedTo: String? = null,
): UUID = transaction { AuthorTable.create(libraryId, renamedTo ?: name, taggedName = name) }

fun newSeries(
    title: String,
    libraryId: UUID,
): UUID = transaction { SeriesTable.create(libraryId, title, taggedName = title) }

fun newBook(
    title: String,
    libraryId: UUID,
    authors: List<UUID> = emptyList(),
    series: List<UUID> = emptyList(),
    narrators: List<String>? = null,
    genres: List<String>? = null,
): UUID =
    transaction {
        val id = BookTable.create(libraryId, title, taggedName = title)
        BookTable.update({ BookTable.id eq id }) {
            it[BookTable.narrators] = narrators
            it[BookTable.genres] = genres
        }
        replaceBookAuthors(id, authors)
        replaceBookSeries(id, series.associateWith { null })
        id
    }

fun newTrack(
    title: String,
    path: String,
    bookId: UUID,
    libraryId: UUID,
    fileModifiedAt: Instant = Instant.EPOCH,
    scanIndex: ULong = 0uL,
    trackNr: Int? = null,
    durationMs: Long = 60_000,
    chapters: List<TrackChapter> = emptyList(),
): UUID =
    transaction {
        TrackTable.insert(
            TrackRow(
                id = UUID.randomUUID(),
                title = title,
                durationMs = durationMs,
                fileModifiedAt = fileModifiedAt,
                path = path,
                book = bookId,
                library = libraryId,
                scanIndex = scanIndex,
                trackNr = trackNr,
                chapters = chapters,
            ),
        )
    }

fun newUser(
    username: String,
    admin: Boolean = false,
    libraries: Map<UUID, LibraryPermissionLevel> = emptyMap(),
): UUID =
    transaction {
        val id =
            UserTable.insert(
                UserRow(
                    id = UUID.randomUUID(),
                    username = username,
                    passwordHash = "hash",
                    admin = admin,
                    tokenVersion = 0,
                ),
            )
        libraries.forEach { (libraryId, level) ->
            LibraryUserTable.insert {
                it[user] = id
                it[library] = libraryId
                it[permissions] = level
            }
        }
        id
    }
