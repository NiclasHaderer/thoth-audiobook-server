package io.thoth.server.database.tables

enum class MetadataLayer {
    FILE,
    AGENT,
    USER,
}

interface LayerField {
    val name: String

    val column: String get() = name.lowercase()
}

interface LayerRow<F : Enum<F>> {
    val claimed: Set<F>
}

// A layer speaks for a field when it holds a value, or when it claimed the field: a claim without a value is a
// deliberate blank that has to hide whatever the layers below say.
fun <R : LayerRow<F>, F : Enum<F>, T> resolve(
    field: F,
    get: R.() -> T?,
    user: R,
    agent: R,
    file: R,
    preferFile: Boolean,
): T? {
    val order = if (preferFile) listOf(user, file, agent) else listOf(user, agent, file)
    return order.firstOrNull { field in it.claimed || it.get() != null }?.get()
}

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
