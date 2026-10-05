package io.thoth.server.database.tables

import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.models.User
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.jdbc.insert
import java.util.UUID

object UserTable : UUIDTable("user") {
    val username = varchar("username", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val admin = bool("admin").default(false)
    val tokenVersion = integer("token_version")
}

data class UserRow(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val admin: Boolean,
    val tokenVersion: Int,
) {
    fun toModel(): User = User(id = id, username = username, admin = admin)

    fun toExternalUser(): ThothDatabaseUser =
        ThothDatabaseUser(
            id = id,
            username = username,
            passwordHash = passwordHash,
            tokenVersion = tokenVersion,
        )
}

fun ResultRow.toUserRow(): UserRow =
    UserRow(
        id = this[UserTable.id].value,
        username = this[UserTable.username],
        passwordHash = this[UserTable.passwordHash],
        admin = this[UserTable.admin],
        tokenVersion = this[UserTable.tokenVersion],
    )

context(_: Transaction)
fun UserTable.insert(row: UserRow): UUID {
    insert {
        it[id] = row.id
        it[username] = row.username
        it[passwordHash] = row.passwordHash
        it[admin] = row.admin
        it[tokenVersion] = row.tokenVersion
    }
    return row.id
}
