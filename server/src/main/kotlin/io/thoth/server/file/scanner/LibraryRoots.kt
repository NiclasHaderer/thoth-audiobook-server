package io.thoth.server.file.scanner

import io.thoth.server.common.extensions.canonical
import io.thoth.server.database.tables.LibraryTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
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
    val combineFileScannerFields: Boolean,
)

class LibraryRoots {
    fun all(): List<LibraryEntityModel> = transaction { LibraryTable.selectAll().map { it.toRoot() } }

    fun of(id: UUID): LibraryEntityModel? =
        transaction {
            LibraryTable
                .selectAll()
                .where { LibraryTable.id eq id }
                .firstOrNull()
                ?.toRoot()
        }

    fun owning(path: Path): LibraryEntityModel? =
        all().firstOrNull { library ->
            library.folders.any {
                path.startsWith(it)
            }
        }

    private fun ResultRow.toRoot() =
        LibraryEntityModel(
            id = this[LibraryTable.id].value,
            name = this[LibraryTable.name],
            folders = this[LibraryTable.folders].map { Path.of(it).canonical() },
            fileScanners = this[LibraryTable.fileScanners].map { it.name },
            combineFileScannerFields = this[LibraryTable.combineFileScannerFields],
        )
}
