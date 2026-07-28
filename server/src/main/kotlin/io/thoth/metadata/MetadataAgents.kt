package io.thoth.metadata

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.server.database.tables.LibraryEntity

class MetadataAgents(
    private val items: List<MetadataAgent>,
) : List<MetadataAgent> by items {
    private val log = logger {}

    fun forLibrary(library: LibraryEntity): MetadataAgent {
        val libraryAgents = library.metadataAgents.map { it.name }
        val agentsToUse = filter { it.name in libraryAgents }
        if (agentsToUse.isEmpty()) {
            log.info {
                "Library does not reference any available metadata agents " +
                    "(available agents: ${map { it.name }}) (library agents: $libraryAgents)"
            }
        }
        return MetadataAgentWrapper(agentsToUse)
    }
}
