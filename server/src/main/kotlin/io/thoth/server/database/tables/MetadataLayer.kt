package io.thoth.server.database.tables

enum class MetadataLayer {
    FILE,
    AGENT,
    USER,
}

fun <T> resolve(
    user: T?,
    agent: T?,
    file: T?,
    preferFile: Boolean,
): T? = user ?: if (preferFile) file ?: agent else agent ?: file

fun resolveLayer(
    userClaims: Boolean,
    agentClaims: Boolean,
    fileNamesSomething: Boolean,
    preferFile: Boolean,
): MetadataLayer =
    when {
        userClaims -> MetadataLayer.USER
        preferFile && fileNamesSomething -> MetadataLayer.FILE
        agentClaims -> MetadataLayer.AGENT
        else -> MetadataLayer.FILE
    }
