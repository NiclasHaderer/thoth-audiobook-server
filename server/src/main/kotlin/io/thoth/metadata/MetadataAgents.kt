package io.thoth.metadata

import io.thoth.server.database.tables.LibraryEntity
import mu.KotlinLogging.logger

class MetadataAgents(
    private val items: List<MetadataAgent>,
) : List<MetadataAgent> by items {
    private val log = logger {}

    fun forLibrary(library: LibraryEntity): MetadataAgent {
        val agentsToUse = filter { agent -> agent.name in library.metadataAgents.map { it.name } }
        if (agentsToUse.isEmpty()) {
            log.warn {
                "Library does not reference any available metadata agents"
                " (available agents: ${map { it.name }})"
                " (library agents: ${library.metadataAgents.map { it.name }})"
            }
        }
        return MetadataAgentWrapper(agentsToUse)
    }
}
