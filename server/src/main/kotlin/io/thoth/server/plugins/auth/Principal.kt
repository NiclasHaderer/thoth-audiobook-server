package io.thoth.server.plugins.auth

import io.ktor.http.HttpMethod
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.routing.RoutingContext
import io.thoth.auth.models.ThothJwtTypes
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.models.LibraryPermissions
import io.thoth.models.UpdatePermissions
import io.thoth.models.UserPermissions
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.UserEntity
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class ThothPrincipalImpl(
    override val userId: UUID,
    override val type: ThothJwtTypes,
) : ThothPrincipal {
    val permissions: UserPermissions
        get() = resolveUserPermissions(userId)
}

fun resolveUserPermissions(userId: UUID): UserPermissions =
    transaction {
        val user = UserEntity.findById(userId) ?: throw ErrorResponse.notFound("User", userId)
        val permissions: List<LibraryPermissions> =
            if (user.admin) {
                LibrariesTable.selectAll().map {
                    LibraryPermissions(
                        id = it[LibrariesTable.id].value,
                        permissions = UpdatePermissions.READ_WRITE,
                        name = it[LibrariesTable.name],
                    )
                }
            } else {
                user.permissions.map { it.toModel() }
            }
        UserPermissions(isAdmin = user.admin, libraries = permissions)
    }

fun jwtToPrincipal(credentials: JWTCredential): ThothPrincipalImpl? {
    val userIdStr = credentials.payload.getClaim("sub").asString() ?: return null
    val userId = UUID.fromString(userIdStr)
    val enumType =
        try {
            val type = credentials.payload.getClaim("type").asString() ?: return null
            ThothJwtTypes.entries.first { it.type == type }
        } catch (_: Exception) {
            return null
        }

    return ThothPrincipalImpl(userId = userId, type = enumType)
}

fun RoutingContext.thothPrincipal(): ThothPrincipalImpl =
    thothPrincipalOrNull()
        ?: throw ErrorResponse.internalError("Could not get principal. Route has to be guarded with one of the Guards")

fun RoutingContext.thothPrincipalOrNull(): ThothPrincipalImpl? = call.principal()

fun RoutingContext.assertLibraryPermissions(vararg libraryIds: UUID) {
    val principal = thothPrincipal()

    val readonlyMethods = listOf(HttpMethod.Head, HttpMethod.Get, HttpMethod.Options)
    val isWrite = !readonlyMethods.contains(call.request.httpMethod)

    libraryIds.forEach { libId ->
        val library =
            principal.permissions.libraries.firstOrNull { allowedLib -> allowedLib.id == libId }
                ?: throw ErrorResponse.forbidden("access", "Library $libId")

        if (isWrite && library.permissions != UpdatePermissions.READ_WRITE) {
            throw ErrorResponse.forbidden("modify", "Library $libId")
        }
    }
}
