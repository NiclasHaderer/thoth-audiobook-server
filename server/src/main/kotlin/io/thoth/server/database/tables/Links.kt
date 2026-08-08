package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

/** Makes the link rows of [owner] exactly [targets]: stale links are deleted, missing ones inserted. */
context(_: Transaction)
fun <T : Table> T.replaceLinks(
    ownerColumn: Column<EntityID<UUID>>,
    owner: UUID,
    linkedColumn: Column<EntityID<UUID>>,
    targets: Collection<UUID>,
) {
    val wanted = targets.toSet()
    val existing = existingLinks(ownerColumn, owner, linkedColumn)
    val stale = existing - wanted
    if (stale.isNotEmpty()) {
        deleteWhere { (ownerColumn eq owner) and (linkedColumn inList stale) }
    }
    insertLinks(ownerColumn, owner, linkedColumn, wanted - existing)
}

/** Adds the link rows of [owner] that are missing from [targets]; existing links are left alone. */
context(_: Transaction)
fun <T : Table> T.addLinks(
    ownerColumn: Column<EntityID<UUID>>,
    owner: UUID,
    linkedColumn: Column<EntityID<UUID>>,
    targets: Collection<UUID>,
) {
    if (targets.isEmpty()) return
    val existing = existingLinks(ownerColumn, owner, linkedColumn)
    insertLinks(ownerColumn, owner, linkedColumn, targets.toSet() - existing)
}

context(_: Transaction)
private fun <T : Table> T.existingLinks(
    ownerColumn: Column<EntityID<UUID>>,
    owner: UUID,
    linkedColumn: Column<EntityID<UUID>>,
): Set<UUID> =
    select(linkedColumn)
        .where { ownerColumn eq owner }
        .mapTo(mutableSetOf()) { it[linkedColumn].value }

context(_: Transaction)
private fun <T : Table> T.insertLinks(
    ownerColumn: Column<EntityID<UUID>>,
    owner: UUID,
    linkedColumn: Column<EntityID<UUID>>,
    targets: Set<UUID>,
) {
    targets.forEach { target ->
        insert {
            it[ownerColumn] = owner
            it[linkedColumn] = target
        }
    }
}
