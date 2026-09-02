package io.thoth.server.plugins.auth

import io.ktor.http.HttpMethod
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.routing.RoutingContext
import io.thoth.auth.utils.ThothPrincipal
import io.thoth.models.LibraryPermissions
import io.thoth.models.LibraryPermissionLevel
import io.thoth.models.UserPermissions
import io.thoth.openapi.ktor.RouteParamsKey
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.LibraryUserTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class ThothPrincipalImpl(
    override val userId: UUID,
) : ThothPrincipal {
    val permissions: UserPermissions = resolveUserPermissions(userId)
}

fun resolveUserPermissions(userId: UUID): UserPermissions =
    transaction {
        val user = userRow(userId) ?: throw ErrorResponse.notFound("User", userId)
        val permissions: List<LibraryPermissions> =
            if (user.admin) {
                LibrariesTable.selectAll().map {
                    LibraryPermissions(
                        id = it[LibrariesTable.id].value,
                        permissions = LibraryPermissionLevel.READ_WRITE,
                        name = it[LibrariesTable.name],
                    )
                }
            } else {
                (LibraryUserTable innerJoin LibrariesTable)
                    .select(LibrariesTable.id, LibrariesTable.name, LibraryUserTable.permissions)
                    .where { LibraryUserTable.user eq userId }
                    .map {
                        LibraryPermissions(
                            id = it[LibrariesTable.id].value,
                            permissions = it[LibraryUserTable.permissions],
                            name = it[LibrariesTable.name],
                        )
                    }
            }
        UserPermissions(isAdmin = user.admin, libraries = permissions)
    }

fun RoutingContext.thothPrincipal(): ThothPrincipalImpl =
    thothPrincipalOrNull()
        ?: throw ErrorResponse.internalError("Could not get principal. Route has to be guarded with one of the Guards")

fun RoutingContext.thothPrincipalOrNull(): ThothPrincipalImpl? = call.principal()

/**
 * Marks a route whose writes only ever touch the calling user's own state - listening progress and
 * the like. Such a route needs library membership, but not write permission on the library itself:
 * a READONLY member still gets to track what they listened to.
 */
interface UserScoped

fun RoutingContext.assertLibraryPermissions(vararg libraryIds: UUID) {
    val principal = thothPrincipal()

    val readonlyMethods = listOf(HttpMethod.Head, HttpMethod.Get, HttpMethod.Options)
    val isWrite =
        !readonlyMethods.contains(call.request.httpMethod) &&
            call.attributes.getOrNull(RouteParamsKey) !is UserScoped

    libraryIds.forEach { libId ->
        val library =
            principal.permissions.libraries.firstOrNull { allowedLib -> allowedLib.id == libId }
                ?: throw ErrorResponse.forbidden("access", "Library $libId")

        if (isWrite && library.permissions != LibraryPermissionLevel.READ_WRITE) {
            throw ErrorResponse.forbidden("modify", "Library $libId")
        }
    }
}
