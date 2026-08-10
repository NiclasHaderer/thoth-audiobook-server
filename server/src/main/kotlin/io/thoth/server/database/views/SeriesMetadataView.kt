package io.thoth.server.database.views

import io.thoth.server.common.exposed.View
import io.thoth.server.common.exposed.layered
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.SeriesAgentMetadataTable
import io.thoth.server.database.tables.SeriesFileMetadataTable
import io.thoth.server.database.tables.SeriesMetadata
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.SeriesUserMetadataTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.select

object SeriesMetadataView : View("SeriesMetadata") {
    val id = javaUUID("id")
    val library = javaUUID("library")
    val title = text("title")
    val totalBooks = integer("totalBooks").nullable()
    val primaryWorks = integer("primaryWorks").nullable()
    val description = text("description").nullable()
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val cover = javaUUID("cover").nullable()

    override fun body() =
        SeriesTable
            .join(LibrariesTable, JoinType.INNER, SeriesTable.library, LibrariesTable.id)
            .join(SeriesFileMetadataTable, JoinType.LEFT, SeriesTable.id, SeriesFileMetadataTable.id)
            .join(SeriesAgentMetadataTable, JoinType.LEFT, SeriesTable.id, SeriesAgentMetadataTable.id)
            .join(SeriesUserMetadataTable, JoinType.LEFT, SeriesTable.id, SeriesUserMetadataTable.id)
            .select(
                SeriesTable.id,
                SeriesTable.library,
                resolve { title },
                resolve { totalBooks },
                resolve { primaryWorks },
                resolve { description },
                resolve { provider },
                resolve { providerID },
                resolve { coverID },
            )

    private fun <T> resolve(pick: SeriesMetadata.() -> Column<T>) =
        layered(
            user = SeriesUserMetadataTable,
            agent = SeriesAgentMetadataTable,
            file = SeriesFileMetadataTable,
            preferFile = LibrariesTable.preferEmbeddedMetadata,
            pick = pick,
        )
}
