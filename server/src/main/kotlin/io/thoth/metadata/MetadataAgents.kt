package io.thoth.metadata

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.models.Library

class MetadataAgents(
    private val items: List<MetadataAgent>,
) : List<MetadataAgent> by items {
    private val log = logger {}

    private val byName by lazy { associateBy { it.name } }

    fun forLibrary(library: Library): MetadataAgentWrapper {
        val libraryAgents = library.metadataAgents.map { it.name }.distinct()
        val agentsToUse = libraryAgents.mapNotNull { byName[it] }
        if (agentsToUse.isEmpty()) {
            log.warn {
                "Library does not reference any available metadata agents " +
                    "(available agents: ${map { it.name }}) (library agents: $libraryAgents)"
            }
        }
        return MetadataAgentWrapper(agentsToUse, combineFields = library.combineMetadataAgentFields)
    }
}
