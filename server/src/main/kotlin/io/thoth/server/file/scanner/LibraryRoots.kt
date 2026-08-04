package io.thoth.server.file.scanner

import io.thoth.server.common.extensions.canonical
import io.thoth.server.database.tables.LibraryEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import java.util.UUID

// An immutable view of one library's folders. Handed to a scan so that editing the library while it walks
// cannot change the tree underneath it, and so worker threads never hold a thread-affine DAO entity.
data class LibraryEntityModel(
    val id: UUID,
    val name: String,
    val folders: List<Path>,
    val fileScanners: List<String>,
)

class LibraryRoots {
    fun all(): List<LibraryEntityModel> = transaction { LibraryEntity.all().map { it.toRoot() } }

    fun of(id: UUID): LibraryEntityModel? = transaction { LibraryEntity.findById(id)?.toRoot() }

    fun owning(path: Path): LibraryEntityModel? = all().firstOrNull { library -> library.folders.any { path.startsWith(it) } }

    private fun LibraryEntity.toRoot() =
        LibraryEntityModel(
            id = id.value,
            name = name,
            folders = folders.map { Path.of(it).canonical() },
            fileScanners = fileScanners.map { it.name },
        )
}
