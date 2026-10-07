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

class Layers<R : LayerRow<F>, F : Enum<F>>(
    val user: R,
    val agent: R,
    val file: R,
    val preferFile: Boolean,
) {
    private val order = if (preferFile) listOf(user, file, agent) else listOf(user, agent, file)

    // A layer speaks for a field when it holds a value, or when it claimed the field: a claim without a value is a
    // deliberate blank that has to hide whatever the layers below say.
    fun <T> resolve(
        field: F,
        get: R.() -> T?,
    ): T? = order.firstOrNull { field in it.claimed || it.get() != null }?.get()
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
