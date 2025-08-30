package io.thoth.models

import java.util.UUID

open class Series(
    val id: UUID,
    val authors: List<NamedId>,
    val title: String,
    val provider: String?,
    val providerID: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val coverID: UUID?,
    val description: String?,
    val genres: List<NamedId>,
    val library: NamedId,
)
