package io.thoth.models

import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable

@Serializable
data class NamedId(val id: UUID_S, val name: String) {
    @Deprecated("Use NamedId(id, name) instead", ReplaceWith("NamedId(id, name)"))
    constructor(name: String, id: UUID_S) : this(id, name)
}

@Serializable
data class TitledId(
    val id: UUID_S,
    val title: String,
) {
    @Deprecated("")
    constructor(
        title: String,
        id: UUID_S,
    ) : this(id, title)
}
