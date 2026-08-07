package io.thoth.server.plugins.auth

import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.tables.UsersTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

// Will not be called concurrently
internal fun requireAnotherAdminExists(userId: UUID) {
    val others =
        UsersTable
            .selectAll()
            .where { (UsersTable.admin eq true) and (UsersTable.id neq userId) }
            .count()

    if (others == 0L) throw ErrorResponse.userError("Cannot remove the only admin user.")
}
