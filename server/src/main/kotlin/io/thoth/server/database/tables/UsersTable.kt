package io.thoth.server.database.tables

import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.models.User
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.insert
import java.util.UUID

object UsersTable : UUIDTable("Users") {
    val username = varchar("username", 255).uniqueIndex()
    val passwordHash = varchar("passwordHash", 255)
    val admin = bool("admin").default(false)
}

data class UserRow(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val admin: Boolean,
) {
    fun toModel(): User = User(id = id, username = username, admin = admin)

    fun toExternalUser(): ThothDatabaseUser =
        ThothDatabaseUser(
            id = id,
            username = username,
            passwordHash = passwordHash,
        )
}

fun ResultRow.toUserRow(): UserRow =
    UserRow(
        id = this[UsersTable.id].value,
        username = this[UsersTable.username],
        passwordHash = this[UsersTable.passwordHash],
        admin = this[UsersTable.admin],
    )

context(_: Transaction)
fun UsersTable.insert(row: UserRow): UUID {
    insert {
        it[id] = row.id
        it[username] = row.username
        it[passwordHash] = row.passwordHash
        it[admin] = row.admin
    }
    return row.id
}
