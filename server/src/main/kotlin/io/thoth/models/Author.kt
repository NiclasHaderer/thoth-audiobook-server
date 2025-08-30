package io.thoth.models

import java.time.LocalDate
import java.util.UUID

open class Author(
    val id: UUID,
    val name: String,
    val provider: String?,
    val providerID: String?,
    val biography: String?,
    val imageID: UUID?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?,
    val library: NamedId,
)
