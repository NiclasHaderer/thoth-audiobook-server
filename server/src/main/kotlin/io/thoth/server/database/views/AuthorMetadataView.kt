package io.thoth.server.database.views

import io.thoth.server.common.exposed.View
import io.thoth.server.common.exposed.layered
import io.thoth.server.database.extensions.timestampMillis
import io.thoth.server.database.tables.AuthorAgentMetadataTable
import io.thoth.server.database.tables.AuthorFileMetadataTable
import io.thoth.server.database.tables.AuthorMetadata
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.AuthorUserMetadataTable
import io.thoth.server.database.tables.LibrariesTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.select

object AuthorMetadataView : View("AuthorMetadata") {
    val id = javaUUID("id")
    val library = javaUUID("library")
    val name = text("name")
    val biography = text("biography").nullable()
    val website = varchar("website", 255).nullable()
    val birthDate = date("birthDate").nullable()
    val bornIn = varchar("bornIn", 255).nullable()
    val deathDate = date("deathDate").nullable()
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val imageId = javaUUID("imageId").nullable()
    val deferDeletionUntil = timestampMillis("deferDeletionUntil").nullable()

    val visible get() = deferDeletionUntil.isNull()

    override fun body() =
        AuthorTable
            .join(LibrariesTable, JoinType.INNER, AuthorTable.library, LibrariesTable.id)
            .join(AuthorFileMetadataTable, JoinType.LEFT, AuthorTable.id, AuthorFileMetadataTable.id)
            .join(AuthorAgentMetadataTable, JoinType.LEFT, AuthorTable.id, AuthorAgentMetadataTable.id)
            .join(AuthorUserMetadataTable, JoinType.LEFT, AuthorTable.id, AuthorUserMetadataTable.id)
            .select(
                AuthorTable.id,
                AuthorTable.library,
                resolve { name },
                resolve { biography },
                resolve { website },
                resolve { birthDate },
                resolve { bornIn },
                resolve { deathDate },
                resolve { provider },
                resolve { providerID },
                resolve { imageID },
                AuthorTable.deferDeletionUntil,
            )

    private fun <T> resolve(pick: AuthorMetadata.() -> Column<T>) =
        layered(
            user = AuthorUserMetadataTable,
            agent = AuthorAgentMetadataTable,
            file = AuthorFileMetadataTable,
            preferFile = LibrariesTable.preferEmbeddedMetadata,
            pick = pick,
        )
}
